terraform {
  backend "azurerm" {}
  
  required_providers {
     azurerm = {
       source  = "hashicorp/azurerm"
       version = "~> 3.76.0"       # AzureRM provider version
     }
     random = {
       source = "hashicorp/random"
     }
     azuread = {
       source  = "hashicorp/azuread"
       version = "1.6.0"
     }
   }
}
