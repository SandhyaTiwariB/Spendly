try {
    $login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"sandhya@spendly.io","password":"password123"}'
    Write-Host "Login SUCCESS! Token: $($login.token.Substring(0, 15))..."
    $token = $login.token
    $headers = @{ "Authorization" = "Bearer $token" }

    # Test Dashboard
    $dash = Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard?period=month" -Method Get -Headers $headers
    Write-Host "Dashboard metrics:"
    Write-Host "Total spent: $($dash.totalSpent)"
    Write-Host "Remaining: $($dash.remainingBudget)"
    Write-Host "Spending overview bars count: $($dash.spendingOverview.Count)"
    Write-Host "Categories count: $($dash.categoryBreakdowns.Count)"

    # Test Statements History
    $history = Invoke-RestMethod -Uri "http://localhost:8080/api/statements/history" -Method Get -Headers $headers
    Write-Host "Statement history status: SUCCESS (records: $($history.Count))"

    # Test Create Transaction (+ Add Expense)
    $newTx = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" -Method Post -Headers $headers -ContentType "application/json" -Body '{"merchant":"Starbucks Coffee","amount":350.00,"category":"Food & Dining","direction":"DEBIT","date":"2026-10-15","isTransfer":false}'
    Write-Host "Add Expense API test: SUCCESS (ID: $($newTx.id), Merchant: $($newTx.merchant), Amount: $($newTx.amount))"

} catch {
    Write-Host "ERROR: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response body: $($reader.ReadToEnd())"
    }
}
