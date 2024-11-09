terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.16"
    }
  }
  required_version = ">= 1.2.0"
}
# Configure AWS Provider
provider "aws" { 
}

# Create IAM role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "hello_world_lambda_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# Attach basic Lambda execution policy to the role
resource "aws_iam_role_policy_attachment" "lambda_basic_policy" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  role       = aws_iam_role.lambda_role.name
}

# Create Lambda function
resource "aws_lambda_function" "hello_world" {
  filename         = "target/lambda.jar"
  function_name    = "HelloWorldJava"
  role            = aws_iam_role.lambda_role.arn
  handler         = "book.TimeoutLambda::handler"
  source_code_hash = filebase64sha256("target/lambda.jar")
  runtime         = "java17"
  
  memory_size = 512
  timeout     = 2

  environment {
    variables = {
      DATABASE_URL = "my-database-url"
    }
  }
}