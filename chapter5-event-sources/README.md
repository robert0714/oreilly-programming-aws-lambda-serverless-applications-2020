# Chapter 5

Example code for Chapter 5, section 1
## Writing Code to Work with Input and Output for Event Sources
The SAM CLI tool that we’ve already used has an interesting command to help with this exercise— `sam local generate-event`. If you run this command, `sam` lists all the services it can generate stub events for, which you can then examine and use to drive your code. For example, part of the output for `sam local generate-event` looks like this:

```bash
sam local generate-event

Usage: sam local generate-event [OPTIONS] COMMAND [ARGS]...

  Generate events for Lambda functions.

Description:

  Generate sample payloads from different event sources
  such as S3, API Gateway, SNS etc. to be sent to Lambda functions.

  This command may not require access to AWS credentials.


Examples:

    Generate event S3 sends to local Lambda function:

        $sam local generate-event s3 [put/delete]


    Customize event by adding parameter flags.:

        $sam local generate-event s3 [put/delete] --help

        $sam local generate-event s3 [put/delete] --bucket <bucket> --key <key>


    Test generated event with serverless function locally!:

        $sam local generate-event s3 [put/delete] --bucket <bucket> --key <key> | sam local invoke -e -


Commands:

    alexa-skills-kit
    alexa-smart-home
    alb
    apigateway
    appsync
    batch
    cloudformation
    cloudfront
    codecommit
    codepipeline
    cognito
    config
    connect
    dynamodb
    cloudwatch
    kinesis
    lex
    lex-v2
    rekognition
    s3
    sagemaker
    ses
    sns
    sqs
    stepfunctions
    workmail
```
Let’s say we’re interested in building a serverless HTTP API. In this case, we use AWS API Gateway as our upstream event source. If we run `sam local generate-event apigateway` the output includes the following:
```bash
sam local generate-event apigateway

Usage: sam local generate-event apigateway [OPTIONS] COMMAND [ARGS]...

Options:
  -h, --help  Show this message and exit.

Commands:
  authorizer          Generates an Amazon API Gateway Authorizer Event
  aws-proxy           Generates an Amazon API Gateway AWS Proxy Event
  http-api-proxy      Generates an Amazon API Gateway Http API Event
  request-authorizer  Generates an Amazon API Gateway Request Authorizer Event
```
Let’s say we’re interested in building a serverless HTTP API. In this case, we use AWS API Gateway as our upstream event source. If we run `sam local generate-event apigateway` the output includes the following:

```bash
Usage: sam local generate-event apigateway [OPTIONS] COMMAND [ARGS]...

Options:
  -h, --help  Show this message and exit.

Commands:
  authorizer          Generates an Amazon API Gateway Authorizer Event
  aws-proxy           Generates an Amazon API Gateway AWS Proxy Event
  http-api-proxy      Generates an Amazon API Gateway Http API Event
  request-authorizer  Generates an Amazon API Gateway Request Authorizer Event
```

It turns out that API Gateway can integrate with Lambda in multiple ways. The one we typically want from this list is the aws-proxy event, where API Gateway acts as a proxy server in front of a Lambda function, so let’s give that a try.
```bash
$ sam local generate-event apigateway aws-proxy

{
  "body": "eyJ0ZXN0IjoiYm9keSJ9",
  "resource": "/{proxy+}",
  "path": "/path/to/resource",
  "httpMethod": "POST",
  "isBase64Encoded": true,
  "queryStringParameters": {
    "foo": "bar"
  },
  ....
```  
This JSON object is a fully baked sample of a typical event a Lambda function receives from API Gateway. In other words, when you set up API Gateway as a trigger for your Lambda function, the event argument that is passed to the Lambda function has this structure.

This sample event doesn’t necessarily help you with the semantics of the integration with API Gateway, but it does give you the shape of the event that your Lambda function receives, which in turn gives you a solid start to writing your code. You can use this JSON object as inspiration, or you can take it a step further and actually embed it in a test—more on that in Chapter 6!
### Configuring a Lambda Event Source
Let’s continue our API Gateway example. The simplest way of defining an API Gateway event source in SAM is to update your Lambda function definition in your [**template.yaml**](template.yaml) as follows:
```yaml
Resources:
  HelloAPIWorldLambda:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: HelloWorldAPIEventSource
      Runtime: java17
      MemorySize: 512
      Handler: book.HelloWorldAPI::handler
      CodeUri: target/lambda.zip
      Events:
        MyApi:
          Type: Api
          Properties:
            Path: /foo
            Method: get
```
Take a look at the `Events` key—that’s where the magic happens. What SAM does in this case is create a whole bunch of resources, including a globally accessible API endpoint (which we get to later in the chapter), but part of what it also does is configure API Gateway to trigger your Lambda function.

SAM can directly configure [many different event sources](https://github.com/aws/serverless-application-model/blob/master/versions/2016-10-31.md#event-source-types). However, if it doesn’t do enough for your requirements, you can always drop down to lower-level CloudFormation resources.

### Understanding Different Event Source Semantics
It would be convenient to say, in fact, that **all** event sources fit into one of these two kinds, but unfortunately there’s a slight complication—there’s a third kind, and that’s Stream/queue event sources, such as:
* Kinesis Data Streams
* DynamoDB Streams
* Simple Queue Service (SQS)

In all three of these cases, we configure the Lambda *platform* to reach out to the upstream service to *poll* for events, as opposed to all the other event sources where we configure a Lambda trigger directly from the upstream service to *push* events to Lambda.

This reversal for stream/queue sources has no impact on the Lambda handler programming model—the method signature is precisely the same. For example, here is the format of a Lambda handler event for SQS (note the array of Records):
```JSON
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "Hello from SQS!",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1523232000000",
        "SenderId": "123456789012",
        "ApproximateFirstReceiveTimestamp": "1523232000001"
      },
      "messageAttributes": {},
      "md5OfBody": "7b270e59b47ff90a553787216d55d91d",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:MyQueue",
      "awsRegion": "us-east-1"
    }
  ]
}
```
Table 5-1. Lambda event source type

|Event Source Type|Event Sources|
|-----------------|-------------|
|Synchronous |API Gateway, Amazon CloudFront (Lambda@Edge), Elastic Load Balancing (Application Load Balancer), Cognito, Lex, Alexa, Kinesis Data Firehose|
|Asynchronous|S3, SNS, Amazon SES, CloudFormation, CloudWatch Logs, CloudWatch Events, CodeCommit, Config|
|Stream/Queue|Kinesis Data Streams, DynamoDB Streams, Simple Queue Service (SQS)|

Stream/queue event sources are also a little different when it comes to error handling (see “Error Handling”). But for now, we know enough about event sources to explore a couple of detailed examples. Let’s dig into our serverless HTTP API.
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


# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **HelloWorldLambdaJava** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name HelloWorldLambdaJava
```