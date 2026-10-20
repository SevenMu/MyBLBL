$ErrorActionPreference = 'Stop'

$zh = @(
    @('technology', [char]0x79D1 + [char]0x6280), @('information', [char]0x8D44 + [char]0x8BAF), @('short_play', [char]0x5C0F + [char]0x5267 + [char]0x573A),
    @('fashion_beauty', [char]0x65F6 + [char]0x5C1A + [char]0x7F8E + [char]0x5986), @('sports', [string]::Join('', [char[]]@(0x4F53,0x80B2,0x8FD0,0x52A8))), @('animal', [string]::Join('', [char[]]@(0x52A8,0x7269))),
    @('vlog', 'vlog'), @('painting', [string]::Join('', [char[]]@(0x7ED8,0x753B))), @('artificial_intelligence', [string]::Join('', [char[]]@(0x4EBA,0x5DE5,0x667A,0x80FD))),
    @('home_decor', [string]::Join('', [char[]]@(0x5BB6,0x88C5,0x623F,0x4EA7))), @('outdoors', [string]::Join('', [char[]]@(0x6237,0x5916,0x6F6E,0x6D41))), @('fitness', [string]::Join('', [char[]]@(0x5065,0x8EAB))),
    @('handcraft', [string]::Join('', [char[]]@(0x624B,0x5DE5))), @('travel', [string]::Join('', [char[]]@(0x65C5,0x6E38,0x51FA,0x884C))), @('rural', [string]::Join('', [char[]]@(0x4E09,0x519C))),
    @('parenting', [string]::Join('', [char[]]@(0x4EB2,0x5B50))), @('health', [string]::Join('', [char[]]@(0x5065,0x5EB7))), @('emotion', [string]::Join('', [char[]]@(0x60C5,0x611F))),
    @('life_interest', [string]::Join('', [char[]]@(0x751F,0x6D3B,0x5174,0x8DA3))), @('life_experience', [string]::Join('', [char[]]@(0x751F,0x6D3B,0x7ECF,0x9A8C))),
    @('setting_home_regions', [string]::Join('', [char[]]@(0x9996,0x9875,0x677F,0x5757,0x7BA1,0x7406))),
    @('guochuang', [string]::Join('', [char[]]@(0x56FD,0x521B))), @('variety', [string]::Join('', [char[]]@(0x7EFC,0x827A)))
)

function Add-Strings($path, $pairs) {
    $content = Get-Content $path -Raw -Encoding UTF8
    $sb = New-Object System.Text.StringBuilder
    foreach ($pair in $pairs) {
        [void]$sb.Append('    <string name="' + $pair[0] + '">' + $pair[1] + '</string>')
        [void]$sb.Append([char]13)
        [void]$sb.Append([char]10)
    }
    $content = $content.Replace('</resources>', $sb.ToString() + '</resources>')
    $full = Join-Path $PWD $path
    [IO.File]::WriteAllText($full, $content, (New-Object System.Text.UTF8Encoding $false))
    Write-Output ('updated: ' + $path)
}

Add-Strings 'app\src\main\res\values\strings.xml' $zh
