####################################################################################################################################
# WEBAPP

resource "aws_cloudwatch_log_group" "MotrWebHandler" {
  count             = "${var.manage_cw_lg_web_lambda ? 1 : 0}"
  name              = "/aws/lambda/${aws_lambda_function.MotrWebHandler.function_name}"
  retention_in_days = "${var.cw_lg_web_lambda_retention}"
  tags {
    Project     = "${var.project}"
    Environment = "${var.environment}"
  }
}

####################################################################################################################################
# SUBSCRIPTION LOADER

resource "aws_cloudwatch_log_group" "MotrSubscriptionLoader" {
  count             = "${var.manage_cw_lg_subscr_lambda ? 1 : 0}"
  name              = "/aws/lambda/${aws_lambda_function.MotrSubscriptionLoader.function_name}"
  retention_in_days = "${var.cw_lg_subscr_lambda_retention}"
  tags {
    Project     = "${var.project}"
    Environment = "${var.environment}"
  }
}

resource "aws_cloudwatch_event_rule" "MotrLoaderStart" {
  name                = "motr-loader-start-${var.environment}"
  description         = "MOTR Loader Start (${var.environment}) event rule | Schedule: ${var.motr_loader_schedule}"
  is_enabled          = "${var.motr_loader_enabled}"
  schedule_expression = "${var.motr_loader_schedule}"
}

resource "aws_cloudwatch_event_target" "MotrLoaderStart" {
  rule      = "${aws_cloudwatch_event_rule.MotrLoaderStart.name}"
  target_id = "${aws_cloudwatch_event_rule.MotrLoaderStart.name}-target"
  arn       = "${aws_lambda_function.MotrSubscriptionLoader.arn}"
}

####################################################################################################################################
# NOTIFIER

resource "aws_cloudwatch_log_group" "MotrNotifier" {
  count             = "${var.manage_cw_lg_notifier_lambda ? 1 : 0}"
  name              = "/aws/lambda/${aws_lambda_function.MotrNotifier.function_name}"
  retention_in_days = "${var.cw_lg_notifier_lambda_retention}"
  tags {
    Project     = "${var.project}"
    Environment = "${var.environment}"
  }
}

resource "aws_cloudwatch_event_rule" "MotrNotifierStart" {
  name                = "motr-notifier-start-${var.environment}"
  description         = "MOTR MotrNotifierStart Start (${var.environment}) event rule | Schedule: ${var.motr_notifier_schedule}"
  is_enabled          = "${var.motr_notifier_enabled}"
  schedule_expression = "${var.motr_notifier_schedule}"
}

resource "aws_cloudwatch_event_target" "MotrNotifierStart" {
  rule      = "${aws_cloudwatch_event_rule.MotrNotifierStart.name}"
  target_id = "${aws_cloudwatch_event_rule.MotrNotifierStart.name}-target"
  arn       = "${aws_lambda_function.MotrNotifier.arn}"
}
