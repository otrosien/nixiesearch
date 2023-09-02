package ai.nixiesearch.index

import ai.nixiesearch.config.StoreConfig
import ai.nixiesearch.config.StoreConfig.LocalStoreConfig
import ai.nixiesearch.config.mapping.IndexMapping
import cats.effect.IO
import cats.effect.kernel.Resource

trait Index {

  def reader(): Resource[IO, IndexReader]
  def writer(): Resource[IO, IndexWriter]
}

object Index {
  val MAPPING_FILE_NAME = "mapping.json"
}