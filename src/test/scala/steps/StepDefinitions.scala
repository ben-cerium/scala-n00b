package steps
import io.cucumber.scala.{EN, ScalaDsl}
import awsUtil.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{
  CursorOp,
  Decoder,
  DecodingFailure,
  Encoder,
  HCursor,
  Json,
  parser
}
import uk.gov.nationalarchives.tre.messages.event.{Producer, Properties}
import uk.gov.nationalarchives.tre.messages.request.judgement.parse.{
  RequestJudgementParse,
  Parameters
}

import uk.gov.nationalarchives.tre.messages.event.Producer.Producer
import uk.gov.nationalarchives.tre.messages.judgmentpackage.available.Status
import uk.gov.nationalarchives.tre.messages.judgmentpackage.available.Status.Status

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_INSTANT

// Setup up the enumeration encoding and decoding for Circe
implicit val producerDecoder: Decoder[Producer.Value] = (c: HCursor) => {
  c.as[String].flatMap {
    case "TRE" => Right(Producer.TRE)
    case "TDR" => Right(Producer.TDR)
    case "FCL" => Right(Producer.FCL)
    case "DRI" => Right(Producer.DRI)
    case other =>
      Left(
        DecodingFailure(
          "failed to decode producer",
          List(
            CursorOp.DownField(other)
          )
        )
      )
  }
}
object ProducerEncoder extends Encoder[Producer] {
  final def apply(producer: Producer): Json = Json.fromString(producer.toString)
}
implicit val producerEncoder: Encoder[Producer.Value] = ProducerEncoder

implicit val statusDecoder: Decoder[Status.Value] = (c: HCursor) => {
  c.as[String].flatMap {
    case "JUDGMENT_PARSE_NO_ERRORS" => Right(Status.JUDGMENT_PARSE_NO_ERRORS)
    case "JUDGMENT_PARSE_WITH_ERRORS" =>
      Right(Status.JUDGMENT_PARSE_WITH_ERRORS)
    case other =>
      Left(
        DecodingFailure(
          "failed to decode status",
          List(
            CursorOp.DownField(other)
          )
        )
      )
  }
}
object StatusEncoder extends Encoder[Status] {
  final def apply(status: Status): Json = Json.fromString(status.toString)
}
implicit val statusEncoder: Encoder[Status.Value] = StatusEncoder

// Steps definitions for Cucumber
class HelloWorldStepDefinitions extends ScalaDsl with EN {
  var concatenatedString: String = ""

  Given("Setup is correct") { () =>
    true
  }

  When("I concatenate {string} and {string}") {
    (string1: String, string2: String) =>
      concatenatedString = string1 + string2
  }

  Then("The result should be {string}") { (string: String) =>
    assert(concatenatedString == string)
  }
}

class FCLStepDefinitions extends ScalaDsl with EN {
  var treOutBucket = ""
  var treInTopicArn = ""
  var requestOriginator = ""
  var requestReference = ""
  var requestJudgmentUri = ""
  var timeNow = ZonedDateTime.now()

  Given(
    "I have access to a {string} that has captured messages from tre-out and can publish to SNS topic {string}"
  ) { (bucket: String, topic: String) =>
    treOutBucket = bucket
    treInTopicArn = topic
    assert(bucketExists(bucket))
  }

  When(
    "I send a valid RequestJudgmentParse with {string} and {string} and {string}"
  ) { (originator: String, reference: String, judgmentUri: String) =>
    requestOriginator = originator
    requestReference = reference
    requestJudgmentUri = judgmentUri

  // Create a RequestJudgmentParse with the params from the feature
  val props = Properties(
    messageType =
      "uk.gov.nationalarchives.tre.messages.judgment.parse.RequestJudgmentParse",
    timestamp = timeNow.format(ISO_INSTANT),
    producer = Producer.FCL,
    function = "fcl-judgment-parse-request",
    executionId = "executionId344",
    parentExecutionId = None
  )
  // Message parameters
  val parameters = Parameters(
    judgmentURI = requestJudgmentUri,
    reference = requestReference,
    originator = Some(requestOriginator)
  )

  val requestJudgmentParseMessage =
    uk.gov.nationalarchives.tre.messages.request.judgement.parse
      .RequestJudgementParse(props, parameters)
  val json = requestJudgmentParseMessage.asJson.toString()
  // println(s"About to send ${json}")

  publishMessageToSnsTopic(json, treInTopicArn)
  }

  Then("after a {int} seconds delay") { (secondsDelay: Int) =>
    Thread.sleep(secondsDelay * 1000)
  }

  Then(
    "I recieve a valid JudgmentPackageAvailable message with the status JUDGMENT_PARSE_NO_ERRORS"
  ) { () =>
    // Look for newest message, it should have the correct status, and matching orginator and reference
    val json = getNewestJudgmentPackageAvailableMessage(
      requestOriginator,
      requestReference,
      treOutBucket
    )

    // println(s"found ${json}")

    var messageRecieved = io.circe.parser.decode[
      uk.gov.nationalarchives.tre.messages.judgmentpackage.available.JudgmentPackageAvailable
    ](json)
    // println(s"${messageRecieved}")

    // Check that the newest message found is newer than the message sent.  Because request judgements can have the same orginator, reference, executionid and even timestamp (and we assume the timestamp supplied is not in the future) the best we can do is assume the judgement available message is newer than the request judgement message.
    assert(
      timeNow.isBefore(
        ZonedDateTime.parse(messageRecieved.right.get.properties.timestamp)
      ),
      "Message found is not newer than message sent"
    )
    assert(
      messageRecieved.right.get.parameters.status == Status.JUDGMENT_PARSE_NO_ERRORS,
      s"Expected status of ${Status.JUDGMENT_PARSE_NO_ERRORS} but got ${messageRecieved.right.get.parameters.status}"
    )
  }
}
