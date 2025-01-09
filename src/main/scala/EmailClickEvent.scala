package com.iterable.poc

import java.time.Instant

case class EmailClickEvent (url: String, timestamp: Instant = Instant.now()) {
  require(url.nonEmpty, "URL cannot be empty")
}