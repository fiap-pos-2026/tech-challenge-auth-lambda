data "terraform_remote_state" "db" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = var.db_infra_state_key
    region = var.aws_region
  }
}

data "terraform_remote_state" "k8s" {
  backend = "s3"

  config = {
    bucket = var.state_bucket
    key    = var.k8s_infra_state_key
    region = var.aws_region
  }
}

data "aws_caller_identity" "current" {}

locals {
  name_prefix   = "${var.project_name}-${var.environment}"
  function_name = "${local.name_prefix}-auth-lambda"

  db_outputs  = data.terraform_remote_state.db.outputs
  k8s_outputs = data.terraform_remote_state.k8s.outputs

  vpc_id             = local.db_outputs.vpc_id
  private_subnet_ids = local.db_outputs.private_subnet_ids
  jdbc_url           = local.db_outputs.jdbc_url
  db_username        = local.db_outputs.rds_username

  api_gateway_id       = local.k8s_outputs.api_gateway_id
  api_gateway_endpoint = local.k8s_outputs.api_gateway_endpoint

  # Issuer = URL pública da própria Lambda no Gateway, exigido pelo authorizer JWT nativo do HTTP API
  jwt_issuer = "${local.api_gateway_endpoint}/auth"
}

check "upstream_outputs_ready" {
  assert {
    condition = (
      try(length(local.private_subnet_ids), 0) > 0 &&
      try(local.jdbc_url, "") != "" &&
      try(local.api_gateway_id, "") != ""
    )
    error_message = "Remote state incompleto. Ordem de apply: tech-challenge-db-infra -> tech-challenge-k8s-infra -> tech-challenge-auth-lambda."
  }
}
