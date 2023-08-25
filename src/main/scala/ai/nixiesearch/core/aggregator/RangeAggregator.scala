package ai.nixiesearch.core.aggregator

import ai.nixiesearch.api.aggregation.Aggregation.{AggRange, RangeAggregation, TermAggregation}
import ai.nixiesearch.config.FieldSchema
import ai.nixiesearch.config.FieldSchema.{FloatFieldSchema, IntFieldSchema}
import ai.nixiesearch.core.Field
import ai.nixiesearch.core.aggregator.AggregationResult.{RangeAggregationResult, RangeCount}
import cats.effect.IO
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.range.{DoubleRange, DoubleRangeFacetCounts, LongRange, LongRangeFacetCounts}
import org.apache.lucene.index.IndexReader

object RangeAggregator {
  def aggregate(
      reader: IndexReader,
      request: RangeAggregation,
      facets: FacetsCollector,
      field: FieldSchema[_ <: Field]
  ): IO[RangeAggregationResult] = field match {
    case int: IntFieldSchema  => intAggregate(reader, request, facets)
    case fl: FloatFieldSchema => floatAggregate(reader, request, facets)
    case other => IO.raiseError(new Exception(s"cannot do range aggregation for a non-numeric field ${field.name}"))
  }

  def intAggregate(
      reader: IndexReader,
      request: RangeAggregation,
      facets: FacetsCollector
  ): IO[RangeAggregationResult] = IO {
    val ranges = request.ranges.map {
      case AggRange.RangeFrom(from)       => new LongRange(s"$from-*", math.round(from), true, Long.MaxValue, false)
      case AggRange.RangeTo(to)           => new LongRange(s"*-$to", Long.MinValue, true, math.round(to), false)
      case AggRange.RangeFromTo(from, to) => new LongRange(s"$from-$to", math.round(from), true, math.round(to), false)
    }
    val counts = new LongRangeFacetCounts(request.field, facets, ranges: _*)
    val buckets = for {
      (count, range) <- counts.getAllChildren(request.field).labelValues.map(_.value.intValue()).zip(request.ranges)
    } yield {
      range match {
        case AggRange.RangeFrom(from)       => RangeCount(Some(from), None, count)
        case AggRange.RangeTo(to)           => RangeCount(None, Some(to), count)
        case AggRange.RangeFromTo(from, to) => RangeCount(Some(from), Some(to), count)
      }
    }
    RangeAggregationResult(buckets.toList)
  }

  def floatAggregate(
      reader: IndexReader,
      request: RangeAggregation,
      facets: FacetsCollector
  ): IO[RangeAggregationResult] = IO {
    val ranges = request.ranges.map {
      case AggRange.RangeFrom(from)       => new DoubleRange(s"$from-*", from, true, Double.MaxValue, false)
      case AggRange.RangeTo(to)           => new DoubleRange(s"*-$to", Double.MinValue, true, to, false)
      case AggRange.RangeFromTo(from, to) => new DoubleRange(s"$from-$to", from, true, to, false)
    }
    val counts = new DoubleRangeFacetCounts(request.field, facets, ranges: _*)
    val buckets = for {
      (count, range) <- counts.getAllChildren(request.field).labelValues.map(_.value.intValue()).zip(request.ranges)
    } yield {
      range match {
        case AggRange.RangeFrom(from)       => RangeCount(Some(from), None, count)
        case AggRange.RangeTo(to)           => RangeCount(None, Some(to), count)
        case AggRange.RangeFromTo(from, to) => RangeCount(Some(from), Some(to), count)
      }
    }
    RangeAggregationResult(buckets.toList)
  }
}