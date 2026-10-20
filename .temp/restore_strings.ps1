$ErrorActionPreference = 'Stop'

$zh = @(
    @('technology', '科技'), @('information', '资讯'), @('short_play', '小剧场'),
    @('fashion_beauty', '时尚美妆'), @('sports', '体育运动'), @('animal', '动物'),
    @('vlog', 'vlog'), @('painting', '绘画'), @('artificial_intelligence', '人工智能'),
    @('home_decor', '家装房产'), @('outdoors', '户外潮流'), @('fitness', '健身'),
    @('handcraft', '手工'), @('travel', '旅游出行'), @('rural', '三农'),
    @('parenting', '亲子'), @('health', '健康'), @('emotion', '情感'),
    @('life_interest', '生活兴趣'), @('life_experience', '生活经验'),
    @('setting_home_regions', '首页板块管理'),
    @('guochuang', '国创'), @('variety', '综艺')
)
$en = @(
    @('technology', 'Technology'), @('information', 'News'), @('short_play', 'Short Play'),
    @('fashion_beauty', 'Fashion &amp; Beauty'), @('sports', 'Sports'), @('animal', 'Animals'),
    @('vlog', 'Vlog'), @('painting', 'Painting'), @('artificial_intelligence', 'AI'),
    @('home_decor', 'Home &amp; Living'), @('outdoors', 'Outdoors'), @('fitness', 'Fitness'),
    @('handcraft', 'Handcraft'), @('travel', 'Travel'), @('rural', 'Rural Life'),
    @('parenting', 'Parenting'), @('health', 'Health'), @('emotion', 'Emotions'),
    @('life_interest', 'Life Hobbies'), @('life_experience', 'Life Tips'),
    @('setting_home_regions', 'Home tabs'),
    @('guochuang', 'Guochuang'), @('variety', 'Variety')
)
$tw = @(
    @('technology', '科技'), @('information', '資訊'), @('short_play', '小劇場'),
    @('fashion_beauty', '時尚美妝'), @('sports', '體育運動'), @('animal', '動物'),
    @('vlog', 'vlog'), @('painting', '繪畫'), @('artificial_intelligence', '人工智能'),
    @('home_decor', '家裝房產'), @('outdoors', '戶外潮流'), @('fitness', '健身'),
    @('handcraft', '手工'), @('travel', '旅遊出行'), @('rural', '三農'),
    @('parenting', '親子'), @('health', '健康'), @('emotion', '情感'),
    @('life_interest', '生活興趣'), @('life_experience', '生活經驗'),
    @('setting_home_regions', '首頁板塊管理'),
    @('guochuang', '國創'), @('variety', '綜藝')
)

function Add-Strings($path, $pairs) {
    $content = Get-Content $path -Raw -Encoding UTF8
    $sb = New-Object System.Text.StringBuilder
    foreach ($pair in $pairs) {
        [void]$sb.Append("    <string name=`"$($pair[0])`">$($pair[1])</string>`r`n")
    }
    $content = $content.Replace('</resources>', $sb.ToString() + '</resources>')
    [IO.File]::WriteAllText("$PWD\$path".Replace('\', '\'), $content, (New-Object System.Text.UTF8Encoding $false))
    Write-Output "updated: $path"
}

Add-Strings 'app\src\main\res\values\strings.xml' $zh
Add-Strings 'app\src\main\res\values-en\strings.xml' $en
Add-Strings 'app\src\main\res\values-zh-rTW\strings.xml' $tw
