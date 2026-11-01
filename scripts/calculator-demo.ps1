param([switch]$Pause)

$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (-not (Test-Path 'target/classes/io/github/alextoday111/projectd/semantic/SemanticMain.class')) {
        throw 'Build the project first: mvn clean verify'
    }
    $semanticMain = 'io.github.alextoday111.projectd.semantic.SemanticMain'
    $source = 'examples/presentation/calculator.d'
    Write-Host 'Project D: calculator'
    Write-Host 'Four functions demonstrate arithmetic, parameters, declarations, and function calls.'
    Write-Host 'The compiler validates and transforms the program; it does not execute it.'
    Write-Host ''
    Get-Content $source
    if ($Pause) { Read-Host 'Press Enter to analyze' | Out-Null }
    Write-Host ''
    Write-Host '1. Check declarations, parameters, and references'
    & java -cp target/classes $semanticMain $source
    if ($LASTEXITCODE -ne 0) { throw 'Calculator semantic analysis failed' }
    if ($Pause) { Read-Host 'Press Enter to compare the trees' | Out-Null }
    Write-Host ''
    Write-Host '2. Compare original and optimized ASTs'
    Write-Host 'The final print folds to integer literals 25, 15, 100, and 4.'
    Write-Host 'Function calls remain calls: the optimizer does not execute or inline functions.'
    & java -cp target/classes $semanticMain --compare $source
    if ($LASTEXITCODE -ne 0) { throw 'Calculator AST comparison failed' }
    Write-Host ''
    Write-Host 'Calculator demonstration completed.'
} finally {
    Pop-Location
}
