terraform {
  backend "azurerm" {}

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "1.6.0"
     }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.45.0"       # AzureRM provider version
    }
  }
}
