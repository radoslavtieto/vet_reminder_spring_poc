output "action_engine_application_insights_connection_string" {
  value     = azurerm_application_insights.main.connection_string
  sensitive = true
}
