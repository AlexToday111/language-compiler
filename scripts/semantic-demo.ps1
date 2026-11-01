param([switch]$Pause)

$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (-not (Test-Path 'target/classes/io/github/alextoday111/projectd/semantic/SemanticMain.class')) {
        throw 'Build the project first: mvn clean verify'
    }
    $semanticMain = 'io.github.alextoday111.projectd.semantic.SemanticMain'
    Write-Host 'Project D: simple examples, semantic checks, and AST optimization'
    Write-Host 'This demonstrates compilation stages. Program execution is not implemented.'
    Write-Host ''
    Write-Host '1. A trivial valid program'
    Get-Content 'examples/valid/01_simple.d'
    if ($Pause) { Read-Host 'Press Enter to analyze' | Out-Null }
    & java -cp target/classes $semanticMain examples/valid/01_simple.d
    if ($LASTEXITCODE -ne 0) { throw 'Simple example failed' }

    Write-Host ''
    Write-Host '2. Constant folding, branch selection, and unreachable code removal'
    Get-Content 'examples/semantic/optimize.d'
    Write-Host 'Look for 11, 4.0, false, ScopedBlock, and removed unreachable prints.'
    if ($Pause) { Read-Host 'Press Enter to compare the trees' | Out-Null }
    & java -cp target/classes $semanticMain --compare examples/semantic/optimize.d
    if ($LASTEXITCODE -ne 0) { throw 'Optimization example failed' }

    Write-Host ''
    Write-Host '3. Semantic errors: each example must fail with exit code 1'
    foreach ($example in @('undeclared', 'duplicate', 'invalid-return', 'invalid-exit')) {
        Write-Host ''
        Write-Host $example
        Get-Content "examples/semantic/$example.d"
        if ($Pause) { Read-Host 'Press Enter to check the error' | Out-Null }
        & java -cp target/classes $semanticMain "examples/semantic/$example.d"
        if ($LASTEXITCODE -ne 1) { throw "Expected a semantic error for $example" }
    }
    Write-Host ''
    Write-Host 'Demonstration completed: the valid examples passed and all four expected errors were detected.'
} finally {
    Pop-Location
}
