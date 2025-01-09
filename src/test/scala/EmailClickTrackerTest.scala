package com.iterable.poc

import org.scalatest.funsuite.AnyFunSuite
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Success

class EmailClickTrackerHourlySpec extends AnyFunSuite {

  def createClickEvent(url: String, timestamp: Instant): EmailClickEvent = {
    EmailClickEvent(url, timestamp)
  }

  def generateClickEvents(url: String, count: Int, timestamp: Instant): Seq[EmailClickEvent] = {
    (1 to count).map(_ => createClickEvent(url, timestamp))
  }

  def generateUniqueClickEvents(count: Int, timestamp: Instant, prefix: Long = 999): Seq[EmailClickEvent] = {
    (1 to count).map { current =>
      val url = s"http://${prefix + current}.example.com"
      createClickEvent(url, timestamp)
    }
  }

  test("EmailClickTracker should estimate unique clicks within and across hourly buckets with variance") {
    val tracker = new EmailClickTracker()

    // Relative Standard Error (RSE) for HyperLogLog with 16 bits
    //val errorVariance = 1 // errorVariance times the standard error due to smaller sample sizes
    val relativeError = 1.0 / math.sqrt(math.pow(2, 16))

    def assertWithinVariance(estimate: Long, expected: Long, message: String): Unit = {
      val lowerBound = (expected * (1 - relativeError)).toLong
      val upperBound = (expected * (1 + relativeError)).toLong
      
      assert(
        estimate >= lowerBound && estimate <= upperBound,
        s"$message\n==> Estimated value $estimate is not within variance bounds [$lowerBound, $upperBound] for expected $expected"
      )

      val BLUE = "\u001B[34m"
      println(s"${BLUE}\nSuccess!\n$message\n==> Estimated value $estimate is within variance bounds [$lowerBound, $upperBound] for expected $expected")
    }

    def assertUniqueClicksPerHour(hour: Instant, expected: Long): Unit = {
      val uniqueClicks = tracker.getUniqueClickCountForHour(hour)
      assertWithinVariance(uniqueClicks, expected, s"Per Hour $hour")
    }

    def assertUniqueClicksAcrossHours(hours: Seq[Instant], expected: Long): Unit = {
      val uniqueClicks = tracker.getUniqueClickCountAcrossHours(hours)
      assertWithinVariance(uniqueClicks, expected, s"Across Hours $hours")
    }

    def recordUniqueClickEvents(count: Int, timestamp: Instant, prefix: Int): Unit = {
      tracker.recordClickEvents(generateUniqueClickEvents(count, timestamp, prefix))
    }

    // Define some timestamps in different hourly buckets
    val now = Instant.now()
    val hour1 = now.truncatedTo(ChronoUnit.HOURS)
    val hour2 = hour1.plus(1, ChronoUnit.HOURS)
    val hour3 = hour1.plus(2, ChronoUnit.HOURS)
    val hour4 = hour1.plus(3, ChronoUnit.HOURS)
    val hour5 = hour1.plus(4, ChronoUnit.HOURS)

    // Record clicks in the first hour
    recordUniqueClickEvents(50000, hour1, 1000000)
    assertUniqueClicksPerHour(hour1, 50000)

    // Record clicks in the second hour
    recordUniqueClickEvents(10000, hour2, 200000)
    assertUniqueClicksPerHour(hour2, 10000)

    // Record clicks in the third hour
    recordUniqueClickEvents(10000, hour3, 300000)
    assertUniqueClicksPerHour(hour3, 10000)

    // Record clicks in the fourth hour
    recordUniqueClickEvents(10000, hour4, 400000)
    assertUniqueClicksPerHour(hour4, 10000)

    // Record clicks in the fifth hour
    recordUniqueClickEvents(10000, hour5, 500000)
    assertUniqueClicksPerHour(hour5, 10000)

    // Unique clicks across hours
    assertUniqueClicksAcrossHours(Seq(hour1, hour2), 60000)
    assertUniqueClicksAcrossHours(Seq(hour1, hour2, hour3), 70000)
    assertUniqueClicksAcrossHours(Seq(hour1, hour2, hour3, hour4), 80000)
    assertUniqueClicksAcrossHours(Seq(hour1, hour2, hour3, hour4, hour5), 90000)
  }
}


