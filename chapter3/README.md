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
or terraform
```
$ terraform init 
$ terraform apply
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


#### Handler: book.HelloWorld::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload \"world\" outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.ListMapLambda::handlerList
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload "[ 1, 2, 3 ]"  outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.ListMapLambda::handlerMap
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload '{ "a" : "x", "b" : "y"}'  outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.ListMapLambda::handlerNestedCollection
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload '[  { "m" : 1, "n" : 2 },   { "x" : 8, "y" : 9 } ]'  outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.PojoLambda::handlerPojo
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload '{ "a" : "Hello Lambda" }'  outputfile.txt
  ``` 
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.StreamLambda::handlerStream
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload \"world\"  outputfile.txt
  ``` 
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    --cli-binary-format raw-in-base64-out \
    --payload  '"hello world"'  outputfile.txt
  ``` 
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.ContextLambda::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    outputfile.txt
  ```  
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.TimeoutLambda::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    outputfile.txt
  ```  
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```
#### Handler: book.EnvVarLambda::handler
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name HelloWorldJava \
    outputfile.txt
  ```  
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```

 
# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **HelloWorldLambdaJava** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name HelloWorldLambdaJava
```
or terraform
```
$ terraform destroy
```
The only example where we don’t use CloudFormation is the very first one earlier in this chapter—the HelloWorld JavaScript function—which can be deleted using the Lambda section of the AWS Web Console.

