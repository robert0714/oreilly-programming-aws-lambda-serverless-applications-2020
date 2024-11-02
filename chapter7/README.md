# Chapter 7

Example code for Chapter 7

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
$ sam deploy --s3-bucket $CF_BUCKET --stack-name chapter7-api --capabilities CAPABILITY_IAM
```

To test:

The above will create a new function in Lambda, so you can test via the Lambda web console,
or via the CLI using `aws lambda invoke`.

## More Information

Please see https://github.com/symphoniacloud/sam-init-HelloWorldLambdaJava for more information.

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

# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **chapter7-api** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name chapter7-api
$ aws s3 rm s3://${CF_BUCKET} --recursive 
```
The only example where we don’t use CloudFormation is the very first one earlier in this chapter—the HelloWorld JavaScript function—which can be deleted using the Lambda section of the AWS Web Console.


#### Handler: book.api.WeatherEventLambda::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name WeatherEventLambda \
    --cli-binary-format raw-in-base64-out \
    --payload "{ \"body\":\"{\\\"locationName\\\":\\\"Brooklyn, NY\\\" , \\\"temperature\\\":91 , \\\"timestamp\\\":1564428897 , \\\"latitude\\\": 40.70, \\\"longitude\\\": -73.99 }\"  }" \
      outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.api.WeatherQueryLambda::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name WeatherQueryLambda \
    --cli-binary-format raw-in-base64-out \
    outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```

## Test
First, let’s send some data. The base of the URL is the one from the API Gateway console, but we append /events. We can call our API using curl, for example, as follows (substitute in your URL):

```bash
$ curl -d '{"locationName":"Brooklyn, NY", "temperature":91,   "timestamp":1564428897, "latitude": 40.70, "longitude": -73.99}' \
  -H "Content-Type: application/json" \
  -X POST  https://ukdi7mhqs8.execute-api.ap-northeast-1.amazonaws.com/Prod/events

Brooklyn, NY

$ curl -d '{"locationName":"Oxford, UK", "temperature":64,  "timestamp":1564428898, "latitude": 51.75, "longitude": -1.25}' \
  -H "Content-Type: application/json" \
  -X POST  https://ukdi7mhqs8.execute-api.ap-northeast-1.amazonaws.com/Prod/events
 
Oxford, UK
```

This has saved two new events to DynamoDB. You can prove that to yourself by clicking on the DynamoDB table from the Serverless Application console, and then clicking on the **Items** tab once you’re in the DynamoDB console

And now we can use the final part of our application—reading from the API. We can use curl for that again, adding /locations to the API Gateway console URL, for example:

```bash
$ curl https://ukdi7mhqs8.execute-api.ap-northeast-1.amazonaws.com/Prod/locations

[{"locationName":"Oxford, UK","temperature":64.0,"timestamp":1564428898,
  "longitude":-1.25,"latitude":51.75},
  {"locationName":"Brooklyn, NY","temperature":91.0,
  "timestamp":1564428897,"longitude":-73.99,"latitude":40.7}]

```
As expected, this returns the list of locations that we’ve stored weather for.

Congratulations! You’ve built your first full serverless application! While it has only one simple feature, think of all the nonfunctional capabilities it has—it auto-scales up to handle a vast load and then back down when not in use, it’s fault-tolerant across multiple availability zones, it has infrastructure that is automatically updated to include critical security patches, and it has a whole lot more besides.

Now let’s look at a different type of application, using a couple of other different AWS services.