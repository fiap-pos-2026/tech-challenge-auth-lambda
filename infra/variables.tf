variable "aws_region" {
  description = "Região AWS (Academy Learner Lab: us-east-1)"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de nomes dos recursos"
  type        = string
  default     = "tech-challenge"
}

variable "environment" {
  description = "Ambiente (homolog | prod)"
  type        = string
  default     = "homolog"
}

variable "state_bucket" {
  description = "Bucket S3 com os states do db-infra e do k8s-infra"
  type        = string
}

variable "db_infra_state_key" {
  description = "Key do state do db-infra"
  type        = string
  default     = "db-infra/homolog/terraform.tfstate"
}

variable "k8s_infra_state_key" {
  description = "Key do state do k8s-infra (API Gateway)"
  type        = string
  default     = "k8s-infra/homolog/terraform.tfstate"
}

variable "lambda_package" {
  description = "Caminho do ZIP gerado por ./gradlew build"
  type        = string
  default     = "../build/distributions/auth-lambda.zip"
}

variable "lambda_memory_size" {
  description = "Memória da Lambda em MB (CPU escala junto; Java precisa de folga no cold start)"
  type        = number
  default     = 512
}

variable "lambda_timeout" {
  description = "Timeout em segundos (cold start + conexão com o RDS)"
  type        = number
  default     = 15
}

variable "lambda_role_name" {
  description = "Role de execução. Academy (voclabs) não permite iam:CreateRole, então usa a LabRole do lab."
  type        = string
  default     = "LabRole"
}

variable "db_password" {
  description = "Senha do banco (mesmo secret TF_VAR_DB_PASSWORD do db-infra)"
  type        = string
  sensitive   = true
}

variable "jwt_private_key" {
  description = "PEM PKCS#8 da chave privada do JWT de cliente (secret AUTH_JWT_PRIVATE_KEY)"
  type        = string
  sensitive   = true
}

variable "jwt_key_id" {
  description = "kid do header do JWT; trocar junto com a chave"
  type        = string
  default     = "auth-lambda-homolog-202610"
}

variable "jwt_expiry" {
  description = "Validade do token (ISO-8601)"
  type        = string
  default     = "PT30M"
}

variable "log_retention_days" {
  description = "Retenção dos logs da Lambda"
  type        = number
  default     = 14
}
