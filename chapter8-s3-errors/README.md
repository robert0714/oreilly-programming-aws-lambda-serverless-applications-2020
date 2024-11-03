# Chapter 5

Example code for Chapter 8, section 1

```
$ mvn package 
$ sam deploy --s3-bucket $CF_BUCKET --stack-name chapter8-s3-errors --capabilities CAPABILITY_IAM

$ ERROR_BUCKET="$(aws cloudformation describe-stack-resource --stack-name chapter8-s3-errors --logical-resource-id ErrorTriggeringBucket --query 'StackResourceDetail.PhysicalResourceId' --output text)"
$ aws s3 cp sampledata.json s3://${ERROR_BUCKET}/sampledata.json

$ sam logs -t -n S3ErroringLambda --stack-name chapter8-s3-errors
```


# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **chapter8-s3-errors** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name chapter8-s3-errors
$ aws s3 rm s3://${CF_BUCKET} --recursive 
```
The only example where we don’t use CloudFormation is the very first one earlier in this chapter—the HelloWorld JavaScript function—which can be deleted using the Lambda section of the AWS Web Console.