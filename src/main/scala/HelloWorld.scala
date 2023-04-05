import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sns.model.SnsException
import software.amazon.awssdk.services.sns.model.InvalidParameterException

def bucketExists(url: String): Boolean =
  val credentialsProvider: ProfileCredentialsProvider =
    ProfileCredentialsProvider.create()
  val region: Region = Region.EU_WEST_2
  val s3: S3Client = S3Client
    .builder()
    .region(region)
    .credentialsProvider(credentialsProvider)
    .build()
  try s3.headBucket(HeadBucketRequest.builder().bucket(url).build())
  catch
    case e: NoSuchBucketException =>
      println(s"Bucket does not exists")
      false
    case e: S3Exception =>
      println(s"Failed with status code = ${e.statusCode()}")
    case e: SdkClientException => println(s"Caught SDK Execption ${e}")
  finally s3.close()
  true

def publishMessageToSnsTopic(message: String, topic: String): Unit =
  println(s"Got ${message} and ${topic}")

  // Build sns client
  // val credentialsProvider: ProfileCredentialsProvider =
  ProfileCredentialsProvider.create()
  // val region: Region = Region.EU_WEST_2
  val sns: SnsClient = SnsClient
    .builder()
    .region(Region.EU_WEST_2)
    .credentialsProvider(ProfileCredentialsProvider.create())
    .build()

  try
    val request: PublishRequest = PublishRequest
      .builder()
      .message(message)
      .topicArn(topic)
      .build()

    val result: PublishResponse = sns.publish(request)

    println(s"Request was ${result}")
  catch
    case e: InvalidParameterException => println(s"${e}")
    case e: Throwable                 => println(s"Got exception ${e}")
  finally sns.close()

@main def commandLine(command: String, args: String*): Unit =

  var a: String = """
{
  "properties" : {
    "messageType" : "uk.gov.nationalarchives.tre.messages.judgment.parse.RequestJudgmentParse",
    "timestampMillis" : "2023-03-29T11:00:12.280Z",
    "function" : "fcl-judgment-parse-request",
    "producer" : "FCL",
    "executionId" : "db8d202b-c174-4066-a482-b39fa52cbd8a",
    "parentExecutionId" : null
  },
  "parameters": {
    "reference": "FCL-BS-24",
    "judgmentURI": "https://tna-caselaw-assets.s3.eu-west-2.amazonaws.com/eat/2022/1/eat_2022_1.docx",
    "originator" : "FCL"
  }
}
"""

  println(s"Got command = ${command} with ${args}")
  val argsArray = args.toArray

  command match
    case "bucket" => bucketExists(args.head)
    // case "snsPublish" => publishMessageToSnsTopic(argsArray(0), argsArray(1))
    case "snsPublish" => publishMessageToSnsTopic(a, argsArray(1))
    case _            => println(s"Unknown command ${command}")

// println(s"Got ${everything}")
//case
//bucketExists(everything.head)
//publishMessageToSnsTopic()

// arn:aws:sns:eu-west-2:882876621099:dev-tre-in
//https://github.com/nationalarchives/da-transform-schemas/blob/feature/DTE-650-implement_caselaw_message/tre_schemas/avro/request-judgment-parse.avsc
