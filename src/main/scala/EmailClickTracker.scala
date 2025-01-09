package com.iterable.poc

import com.twitter.algebird.{HyperLogLog, HyperLogLogMonoid, HLL}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.mutable

import com.iterable.poc.EmailClickEvent

// Not shown in this example, but to store the HLL object in a database, you can:
// Steps
// Serialize the HLL object into a byte array or base64 string for storage in the database.
// Store the serialized data in a BYTEA column (for PostgreSQL) or a similar binary/text column type.
// Deserialize the HLL when needed for computation.

// From the Algebird documentation: 
// Error is about 1.04/sqrt(2^{bits}), so you want something like 12 bits for 1% error
// which means each HLLInstance is about 2^{12} = 4kb per instance. 
// See https://docs.google.com/document/d/1aq_5LL8dqoQmY28F3GG_sNFpVyC_fbei/edit#heading=h.pb2i8c6gvrdy

class EmailClickTracker(bits: Int = 16) {
  private val hllMonoid = new HyperLogLogMonoid(bits)
  private val hourlyData: mutable.Map[Instant, HLL] = mutable.Map.empty

  def recordClickEvents(events: Seq[EmailClickEvent]): Unit = {
    events.foreach(recordClick)
  }

  // Records an email click in the corresponding hour bucket.
  def recordClick(emailClick: EmailClickEvent): Unit = {
    // Truncate the timestamp to the nearest hour
    val bucket = emailClick.timestamp.truncatedTo(ChronoUnit.HOURS)
    // Hash the email and add it to the HyperLogLog
    val hashedEmail = hllMonoid.toHLL(emailClick.url.getBytes)
    // Add the hashed email to the corresponding hour bucket
    val currentHLL = hourlyData.getOrElse(bucket, hllMonoid.zero)
    hourlyData(bucket) = hllMonoid.plus(currentHLL, hashedEmail)
  }

  // Returns an estimate of unique clicks for a specific hour bucket.
  def getUniqueClickCountForHour(hour: Instant): Long = {
    hourlyData.get(hour.truncatedTo(ChronoUnit.HOURS)).map(_.approximateSize.estimate).getOrElse(0L)
  }

  // Returns an estimate of unique clicks across multiple hour buckets.
  def getUniqueClickCountAcrossHours(hours: Seq[Instant]): Long = {
    val combinedHLL = hours.map(hourlyData.getOrElse(_, hllMonoid.zero)).reduce(hllMonoid.plus)
    combinedHLL.approximateSize.estimate
  }

  // Gets all available hour buckets.
  def getHourBuckets: Seq[Instant] = hourlyData.keys.toSeq.sorted
}
