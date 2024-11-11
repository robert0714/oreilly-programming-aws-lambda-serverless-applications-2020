# Provider configuration
provider "aws" { 
}

# DynamoDB table
resource "aws_dynamodb_table" "locations_table" {
  name         = "locations_table"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "locationName"

  attribute {
    name = "locationName"
    type = "S"
  }
}

# Lambda function IAM role
resource "aws_iam_role" "lambda_role" {
  name = "weather_lambda_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# IAM policy for WeatherEventLambda
resource "aws_iam_role_policy" "event_lambda_policy" {
  name = "weather_event_lambda_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:PutItem",
          "dynamodb:DeleteItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem"
        ]
        Resource = [aws_dynamodb_table.locations_table.arn]
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = ["arn:aws:logs:*:*:*"]
      }
    ]
  })
}

# IAM policy for WeatherQueryLambda
resource "aws_iam_role_policy" "query_lambda_policy" {
  name = "weather_query_lambda_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:Scan",
          "dynamodb:Query"
        ]
        Resource = [aws_dynamodb_table.locations_table.arn]
      }
    ]
  })
}

# Lambda functions
resource "aws_lambda_function" "weather_event_lambda" {
  filename      = "target/lambda.zip"
  function_name = "WeatherEventLambda"
  role          = aws_iam_role.lambda_role.arn
  handler       = "book.api.WeatherEventLambda::handler"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 25

  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
    }
  }
}

resource "aws_lambda_function" "weather_query_lambda" {
  filename      = "target/lambda.zip"
  function_name = "WeatherQueryLambda"
  role          = aws_iam_role.lambda_role.arn
  handler       = "book.api.WeatherQueryLambda::handler"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 25

  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
    }
  }
}

# API Gateway
resource "aws_api_gateway_rest_api" "weather_api" {
  name        = "WeatherAPI"
  description = "Weather API Gateway"
}

# API Gateway resource for /events
resource "aws_api_gateway_resource" "events" {
  rest_api_id = aws_api_gateway_rest_api.weather_api.id
  parent_id   = aws_api_gateway_rest_api.weather_api.root_resource_id
  path_part   = "events"
}

# API Gateway resource for /locations
resource "aws_api_gateway_resource" "locations" {
  rest_api_id = aws_api_gateway_rest_api.weather_api.id
  parent_id   = aws_api_gateway_rest_api.weather_api.root_resource_id
  path_part   = "locations"
}

# API Gateway methods
resource "aws_api_gateway_method" "events_post" {
  rest_api_id   = aws_api_gateway_rest_api.weather_api.id
  resource_id   = aws_api_gateway_resource.events.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "locations_get" {
  rest_api_id   = aws_api_gateway_rest_api.weather_api.id
  resource_id   = aws_api_gateway_resource.locations.id
  http_method   = "GET"
  authorization = "NONE"
}

# Lambda integrations
resource "aws_api_gateway_integration" "events_lambda" {
  rest_api_id             = aws_api_gateway_rest_api.weather_api.id
  resource_id             = aws_api_gateway_resource.events.id
  http_method             = aws_api_gateway_method.events_post.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.weather_event_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "locations_lambda" {
  rest_api_id             = aws_api_gateway_rest_api.weather_api.id
  resource_id             = aws_api_gateway_resource.locations.id
  http_method             = aws_api_gateway_method.locations_get.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.weather_query_lambda.invoke_arn
}

# Lambda permissions for API Gateway
resource "aws_lambda_permission" "api_gw_event" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weather_event_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.weather_api.execution_arn}/*/POST/events"
}

resource "aws_lambda_permission" "api_gw_query" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.weather_query_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.weather_api.execution_arn}/*/GET/locations"
}

# API Gateway deployment
resource "aws_api_gateway_deployment" "api_deployment" {
  depends_on = [
    aws_api_gateway_integration.events_lambda,
    aws_api_gateway_integration.locations_lambda
  ]

  rest_api_id = aws_api_gateway_rest_api.weather_api.id
}

# API Gateway stage
resource "aws_api_gateway_stage" "api_stage" {
  deployment_id = aws_api_gateway_deployment.api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.weather_api.id
  stage_name    = "Prod"
}

# API Gateway resource policy to allow public access
resource "aws_api_gateway_rest_api_policy" "api_policy" {
  rest_api_id = aws_api_gateway_rest_api.weather_api.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = "*"
        Action    = "execute-api:Invoke"
        Resource  = "${aws_api_gateway_rest_api.weather_api.execution_arn}/*"
      }
    ]
  })
}

# Output the API endpoint URL
output "api_endpoint" {
  value = aws_api_gateway_stage.api_stage.invoke_url
}