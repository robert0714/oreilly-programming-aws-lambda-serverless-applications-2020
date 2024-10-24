# Chapter 3

Example from Chapter 4 - this builds a Lambda artifact in a zip-file format, and also
uses the reproducible build Maven plugin.

## Prerequisites

* Created: An AWS account, and an S3 bucket for storing temporary deployment artifacts (referred to as $CF_BUCKET below)
* Installed: AWS CLI, Maven, SAM CLI
* Configured: AWS credentials in your terminal

## Usage

To build:

```
$ mvn package
```

To inspect:

```
$ zipinfo -1 target/lambda.zip
```

To deploy:

```
$ sam deploy --s3-bucket $CF_BUCKET --stack-name ChapterFour --capabilities CAPABILITY_IAM
```

To list the stack resources:
```
$ aws cloudformation list-stack-resources --stack-name ChapterFour
```

To test:

The above will create a new function in Lambda, so you can test via the Lambda web console,
or via the CLI using `aws lambda invoke`.
 
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name ChapterFour-HelloWorldLambda-KmcZ2jVSZMKV \
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

# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **HelloWorldLambdaJava** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name ChapterFour 
```
The only example where we don’t use CloudFormation is the very first one earlier in this chapter—the HelloWorld JavaScript function—which can be deleted using the Lambda section of the AWS Web Console.