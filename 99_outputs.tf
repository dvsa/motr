####################################################################################################################################
# OUTPUTS

output "api_gateway_url" {
  value = "https://${aws_api_gateway_rest_api.MotrWeb.id}.execute-api.${var.aws_region}.amazonaws.com/${var.environment}/"
}
