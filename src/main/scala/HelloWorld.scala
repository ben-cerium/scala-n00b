import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

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
      false
  finally s3.close()
  true

@main def commandLine(everything: String*): Unit =
  // println(s"Got ${everything}")
  bucketExists(everything.head)
