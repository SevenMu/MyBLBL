$ErrorActionPreference = 'Stop'

$en = @(
    @('technology', 'Technology'), @('information', 'News'), @('short_play', 'Short Play'),
    @('fashion_beauty', 'Fashion and Beauty'), @('sports', 'Sports'), @('animal', 'Animals'),
    @('vlog', 'Vlog'), @('painting', 'Painting'), @('artificial_intelligence', 'AI'),
    @('home_decor', 'Home and Living'), @('outdoors', 'Outdoors'), @('fitness', 'Fitness'),
    @('handcraft', 'Handcraft'), @('travel', 'Travel'), @('rural', 'Rural Life'),
    @('parenting', 'Parenting'), @('health', 'Health'), @('emotion', 'Emotions'),
    @('life_interest', 'Life Hobbies'), @('life_experience', 'Life Tips'),
    @('setting_home_regions', 'Home tabs'),
    @('guochuang', 'Guochuang'), @('variety', 'Variety')
)
$tw = @(
    @('technology', 'KEJI'), @('information', 'ZIXUN'), @('short_play', 'XIAOJUCHANG'),
    @('fashion_beauty', 'SHISHANGMEIZHUANG'), @('sports', 'TIYUYUNDONG'), @('animal', 'DONGWU'),
    @('vlog', 'vlog'), @('painting', 'HUIHUA'), @('artificial_intelligence', 'RENGONGZHINENG'),
    @('home_decor', 'JIAZHUANGFANGCHAN'), @('outdoors', 'HUWAILIUCHAO'), @('fitness', 'JIANSHEN'),
    @('handcraft', 'SHOUGONG'), @('travel', 'LVYOUCHUXING'), @('rural', 'SANNONG'),
    @('parenting', 'QINZI'), @('health', 'JIANKANG'), @('emotion', 'QINGGAN'),
    @('life_interest', 'SHENGHUOXINGQU'), @('life_experience', 'SHENGHUOJINGYAN'),
    @('setting_home_regions', 'SHOUYEBANKUAIGUANLI'),
    @('guochuang', 'GUOCHUANG'), @('variety', 'ZONGYI')
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

Add-Strings 'app\src\main\res\values-en\strings.xml' $en
Add-Strings 'app\src\main\res\values-zh-rTW\strings.xml' $tw
