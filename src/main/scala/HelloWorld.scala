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
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ListObjectsResponse
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.core.ResponseBytes
import scala.jdk.CollectionConverters._
import com.jayway.jsonpath.JsonPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory

def bucketExists(url: String): Boolean =
  val s3: S3Client = S3Client
    .builder()
    .region(Region.EU_WEST_2)    .credentialsProvider(ProfileCredentialsProvider.create())
    .build()

  try s3.headBucket(HeadBucketRequest.builder().bucket(url).build())
  catch
    case e: NoSuchBucketException =>
      println(s"Bucket does not exists")
      return false
    case e: S3Exception =>
      println(s"Failed with status code = ${e.statusCode()}")
    case e: SdkClientException => println(s"Caught SDK Execption ${e}")
  finally s3.close()
  true

/** Get the newest message from S3 from the path
  * JudgmentPackageAvailableMessage/PRODUCER/REFERENCE Assume that the newest
  * record is the newest object containing the last JSON object
  * @param producer
  *   A RequestJudgmentParse producer
  * @param reference
  *   A RequestJudgmentParse reference
  * @bucketName
  *   A bucket to get the message from
  * @return
  *   a JSON String which is the message
  */
def getNewestJudgmentPackageAvailableMessage(
    producer: String,
    reference: String,
    bucketName: String
): String =
  val objects = listBucket(
    bucketName,
    "judgmentpackage.available.JudgmentPackageAvailable/" + producer + "/" + reference
  )
  // Get the newest one, get the key (object name) and fetch the object
  val newestObjectKey = objects.sortBy(_.lastModified()).last.key()
  getObjectAsString(bucketName, newestObjectKey)

/** Return the contents of a buckets
  * @param bucketName
  * @param path
  *   path (or prefix) to get. Defaults to ""
  * @return
  *   Scala list of S3Objects
  */
def listBucket(bucketName: String, path: String = ""): List[S3Object] =
  val s3: S3Client = S3Client
    .builder()
    .region(Region.EU_WEST_2)
    .credentialsProvider(ProfileCredentialsProvider.create())
    .build()
  val listObjects: ListObjectsRequest =
    ListObjectsRequest.builder().bucket(bucketName).prefix(path).build()
  val res: ListObjectsResponse = s3.listObjects(listObjects)
  res.contents().asScala.toList

/** Return an object in a bucket as a string
  * @param bucketName
  * @param key
  * @return
  *   The object as UTF8 encoded string
  */
def getObjectAsString(bucketName: String, key: String): String =
  val s3: S3Client = S3Client
    .builder()
    .region(Region.EU_WEST_2)
    .credentialsProvider(ProfileCredentialsProvider.create())
    .build()

  val getObjectRequest: GetObjectRequest =
    GetObjectRequest.builder().bucket(bucketName).key(key).build()

  val res: ResponseBytes[GetObjectResponse] =
    s3.getObjectAsBytes(getObjectRequest)
  res.asUtf8String()

def publishMessageToSnsTopic(message: String, topic: String): Unit =
  val logger: Logger = LoggerFactory.getLogger("NameofApp")
  logger.info(s"Got message ${message}")

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
  val logger: Logger = LoggerFactory.getLogger("NameofApp")
  var a: String = """
{
  "properties" : {
    "messageType" : "uk.gov.nationalarchives.tre.messages.judgment.parse.RequestJudgmentParse",
    "timestampMillis" : "2023-03-29T11:00:12.280Z",
    "function" : "fcl-judgment-parse-request",
    "producer" : "FCL",
    "executionId" : "db8d202b--c174-4066-a482-b39fa52cbd8a",
    "parentExecutionId" : null
  },
  "parameters": {
    "reference": "FCL-BS-26",
    "judgmentURI": "https://tna-caselaw-assets.s3.eu-west-2.amazonaws.com/eat/2022/1/eat_2022_1.docx",
    "originator" : "FCL"
  }
}
"""

  logger.info(s"Got command = ${command} with ${args}")
  val argsArray = args.toArray

  command match
    case "bucket" => bucketExists(args.head)
    // case "snsPublish" => publishMessageToSnsTopic(argsArray(0), argsArray(1))
    case "snsPublish" => publishMessageToSnsTopic(a, argsArray(1))
    // case "log" => testLogging()
    // case "listBucket" => listBucket("pte-bs-tre-tre-out-capture")
    case _ => println(s"Unknown command ${command}")

// println(s"Got ${everything}")
//case
//bucketExists(everything.head)
//publishMessageToSnsTopic()

// arn:aws:sns:eu-west-2:882876621099:dev-tre-in
//https://github.com/nationalarchives/da-transform-schemas/blob/feature/DTE-650-implement_caselaw_message/tre_schemas/avro/request-judgment-parse.avsc
