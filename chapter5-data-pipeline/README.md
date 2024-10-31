# Chapter 5

Example code for Chapter 5, section 3

## Prerequisites

* Created: An AWS account, and an S3 bucket for storing temporary deployment artifacts (referred to as $CF_BUCKET below)
* Installed: AWS CLI, Maven, SAM CLI
* Configured: AWS credentials in your terminal

## Usage

To build:

```
$ mvn package
```

To deploy (feel free to change the name of the stack!):

```
$ sam deploy --s3-bucket $CF_BUCKET --stack-name chapter-five-data-pipeline --capabilities CAPABILITY_IAM
```

To test:


```
$ PIPELINE_BUCKET="$(aws cloudformation describe-stack-resource --stack-name chapter-five-data-pipeline --logical-resource-id PipelineStartBucket --query 'StackResourceDetail.PhysicalResourceId' --output text)"
$ aws s3 cp sampledata.json s3://${PIPELINE_BUCKET}/sampledata.json
```
Now look at the logs for the **SingleEventLambda** function, and you’ll see, after a few seconds, each of the weather events separately logged.

# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **ChapterFiveApi** stack, run the following:
```bash
$ PIPELINE_BUCKET="$(aws cloudformation describe-stack-resource --stack-name chapter-five-data-pipeline --logical-resource-id PipelineStartBucket --query 'StackResourceDetail.PhysicalResourceId' --output text)"
$ aws s3 rm s3://${PIPELINE_BUCKET}/sampledata.json
$ aws s3 rm s3://${PIPELINE_BUCKET} --recursive
$ aws cloudformation delete-stack --stack-name  chapter-five-data-pipeline
```