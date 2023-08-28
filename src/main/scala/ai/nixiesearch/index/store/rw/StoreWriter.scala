package ai.nixiesearch.index.store.rw

import ai.nixiesearch.config.mapping.IndexMapping
import ai.nixiesearch.core.{Document, Logging}
import ai.nixiesearch.core.Field.*
import ai.nixiesearch.core.codec.*
import ai.nixiesearch.core.nn.model.BiEncoderCache
import ai.nixiesearch.index.store.LocalStore.DirectoryMapping
import cats.effect.IO
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.MMapDirectory
import org.apache.lucene.document.Document as LuceneDocument

import java.util

case class StoreWriter(
    mapping: IndexMapping,
    writer: IndexWriter,
    directory: MMapDirectory,
    analyzer: Analyzer,
    encoders: BiEncoderCache
) extends Logging {
  lazy val textFieldWriter     = TextFieldWriter(encoders)
  lazy val textListFieldWriter = TextListFieldWriter()
  lazy val intFieldWriter      = IntFieldWriter()
  lazy val floatFieldWriter    = FloatFieldWriter()

  def addDocuments(docs: List[Document]): Unit = {
    val all = new util.ArrayList[LuceneDocument]()
    docs.foreach(doc => {
      val buffer = new LuceneDocument()
      doc.fields.foreach {
        case field @ TextField(name, _) =>
          mapping.textFields.get(name) match {
            case None          => logger.warn(s"text field '$name' is not defined in mapping")
            case Some(mapping) => textFieldWriter.write(field, mapping, buffer)
          }
        case field @ TextListField(name, value) =>
          mapping.textListFields.get(name) match {
            case None          => logger.warn(s"text[] field '$name' is not defined in mapping")
            case Some(mapping) => textListFieldWriter.write(field, mapping, buffer)
          }
        case field @ IntField(name, value) =>
          mapping.intFields.get(name) match {
            case None          => logger.warn(s"int field '$name' is not defined in mapping")
            case Some(mapping) => intFieldWriter.write(field, mapping, buffer)
          }
        case field @ FloatField(name, value) =>
          mapping.floatFields.get(name) match {
            case None          => logger.warn(s"float field '$name' is not defined in mapping")
            case Some(mapping) => floatFieldWriter.write(field, mapping, buffer)
          }
      }
      val finalized = buffer // fc.build(buffer)
      all.add(finalized)
    })
    writer.addDocuments(all)
  }

  def flush(): IO[Unit] = IO(writer.commit())

  def close(): IO[Unit] = info(s"closing index writer for index '${mapping.name}'") *> IO(writer.close())
}

object StoreWriter {
  def create(dm: DirectoryMapping): IO[StoreWriter] = for {
    config   <- IO(new IndexWriterConfig(dm.analyzer))
    writer   <- IO(IndexWriter(dm.dir, config))
    encoders <- BiEncoderCache.create(dm.mapping).allocated.map(_._1)
  } yield {
    StoreWriter(dm.mapping, writer, dm.dir, dm.analyzer, encoders)
  }
}
