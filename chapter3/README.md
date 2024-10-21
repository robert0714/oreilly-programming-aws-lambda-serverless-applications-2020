# Chapter 3

Various example Lambda functions from Chapter 3

## Prerequisites

* Created: An AWS account, and an S3 bucket for storing temporary deployment artifacts (referred to as $CF_BUCKET below)
* Installed: AWS CLI, Maven, SAM CLI
* Configured: AWS credentials in your terminal

## Usage

To build:

```
$ mvn package
```

To deploy:

```
$ sam deploy --s3-bucket $CF_BUCKET --stack-name HelloWorldLambdaJava --capabilities CAPABILITY_IAM
```

To test:

The above will create a new function in Lambda, so you can test via the Lambda web console,
or via the CLI using `aws lambda invoke`.

* See:
  * https://github.com/aws-samples/serverless-test-samples/tree/main/java-test-samples/apigw-lambda-list-s3-buckets#project-contents
  * https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-core/RELEASE.CHANGELOG.md
  * https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-tests
### Test 
  * https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-using-invoke.html
  * https://github.com/aws-samples/serverless-test-samples/blob/main/java-test-samples/apigw-lambda-list-s3-buckets/README.md  
  * https://testcontainers.com/guides/testing-aws-service-integrations-using-localstack/
    * https://github.com/testcontainers/tc-guide-testing-aws-service-integrations-using-localstack
  * https://testcontainers.com/guides/testing-aws-service-integrations-using-localstack/  
    * https://github.com/aosolorzano/city-tasks-lambda-native-with-dynamodb
  * https://docs.aws.amazon.com/lambda/latest/dg/testing-guide.html
  * https://docs.aws.amazon.com/zh_tw/codebuild/latest/userguide/sample-lambda-sam-gradle.html