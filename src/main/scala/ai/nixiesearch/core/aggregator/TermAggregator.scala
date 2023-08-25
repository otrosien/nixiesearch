package ai.nixiesearch.core.aggregator

import ai.nixiesearch.api.aggregation.Aggregation.TermAggregation
import ai.nixiesearch.config.FieldSchema
import ai.nixiesearch.core.Field
import ai.nixiesearch.core.aggregator.AggregationResult.{TermAggregationResult, TermCount}
import cats.effect.IO
import org.apache.lucene.index.{DocValues, IndexReader}
import org.apache.lucene.facet.{FacetsCollector, FacetsConfig, StringDocValuesReaderState, StringValueFacetCounts}

object TermAggregator {
  def aggregate(
      reader: IndexReader,
      request: TermAggregation,
      facets: FacetsCollector,
      field: FieldSchema[_ <: Field]
  ): IO[TermAggregationResult] = {
    field match {
      case _: FieldSchema.TextFieldSchema     => IO(doAggregate(reader, request, facets))
      case _: FieldSchema.TextListFieldSchema => IO(doAggregate(reader, request, facets))
      case other => IO.raiseError(new Exception("term aggregation only works on text and text[] fields"))
    }
  }

  def doAggregate(reader: IndexReader, request: TermAggregation, facets: FacetsCollector): TermAggregationResult = {
    val state  = new StringDocValuesReaderState(reader, request.field)
    val counts = StringValueFacetCounts(state, facets)
    val top    = counts.getTopChildren(request.size, request.field)
    TermAggregationResult(top.labelValues.toList.map(lv => TermCount(lv.label, lv.value.intValue())))
  }

}