# DATA DIVISION
$cprAttribute='employeeID'
$outFile='c:/out.csv'

# PROCEDURE DIVISION
$users = Get-ADUser -Properties $cprAttribute -Filter {$cprAttribute -like '*'}

$sb = [System.Text.StringBuilder]::new()

for ($i=0; $i -lt $users.length; $i++) {
  [void]$sb.Append($users[$i].ObjectGUID)
  [void]$sb.Append(';')
  [void]$sb.Append($users[$i].SamAccountName)
  [void]$sb.Append(';')
  [void]$sb.AppendLine($users[$i].$cprAttribute)
}

$sb.ToString() |Out-File -FilePath $outFile
