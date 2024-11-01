# Chapter 6

Example code for Chapter 6

For more details, see the chapter in the book.

If your integration tests fail (running `mvn verify`) then see the note in the chapter about explicitly setting the region in your `~/.aws/config` file under a section named `[profile-name]` or `[default]` if you're using the default AWS profile.

To test (feel free to change the name of the stack!):

```bash
$ mvn verify -Dintegration.test.code.bucket=$CF_BUCKET -Dintegration.test.stack.name=chapter-six-integration-tests
```


# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **ChapterFiveApi** stack, run the following:
```bash
$ PIPELINE_BUCKET="$(aws cloudformation describe-stack-resource --stack-name chapter-six-integration-tests --logical-resource-id PipelineStartBucket --query 'StackResourceDetail.PhysicalResourceId' --output text)"
$ aws s3 rm s3://${PIPELINE_BUCKET}/sampledata.json
$ aws s3 rm s3://${PIPELINE_BUCKET} --recursive
$ aws cloudformation delete-stack --stack-name  chapter-six-integration-tests
```