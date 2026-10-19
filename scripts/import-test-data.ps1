param(
    [string]$BaseUrl = 'http://127.0.0.1:8088',
    [switch]$GenerateAiActivity
)

$ErrorActionPreference = 'Stop'
$prefix = '【测试】'

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Uri,
        [object]$Body
    )

    $arguments = @{
        Method = $Method
        Uri = $Uri
        ContentType = 'application/json; charset=utf-8'
        NoProxy = $true
    }
    if ($null -ne $Body) {
        $arguments.Body = $Body | ConvertTo-Json -Depth 8
    }
    Invoke-RestMethod @arguments
}

function Sync-Records {
    param(
        [Parameter(Mandatory)] [string]$Endpoint,
        [Parameter(Mandatory)] [string]$Key,
        [Parameter(Mandatory)] [array]$Records
    )

    $response = Invoke-JsonRequest -Method Get -Uri "$BaseUrl$Endpoint"
    $existing = [System.Collections.Generic.List[object]]::new()
    foreach ($item in $response) {
        $existing.Add($item)
    }
    $synced = @()
    foreach ($record in $Records) {
        $keyValue = $record.PSObject.Properties[$Key].Value
        $matches = [System.Collections.Generic.List[object]]::new()
        foreach ($item in $existing) {
            if ($item.PSObject.Properties[$Key].Value -eq $keyValue) {
                $matches.Add($item)
            }
        }
        $match = if ($matches.Count -gt 0) { $matches[0] } else { $null }
        if ($null -eq $match) {
            $synced += Invoke-JsonRequest -Method Post -Uri "$BaseUrl$Endpoint" -Body $record
        }
        else {
            $synced += Invoke-JsonRequest -Method Put -Uri "$BaseUrl$Endpoint/$($match.id)" -Body $record
            if ($matches.Count -gt 1) {
                foreach ($duplicate in $matches | Select-Object -Skip 1) {
                    Invoke-JsonRequest -Method Delete -Uri "$BaseUrl$Endpoint/$($duplicate.id)" | Out-Null
                }
            }
        }
    }
    return $synced
}

$places = @(
    [pscustomobject]@{ name="$prefix 东门"; category='ENTRANCE'; building='东门'; floor='室外'; status='OPEN'; longitude=113.35410; latitude=23.12910; entranceHint='从东门闸机进入，右侧设有新生咨询点'; description='联调测试数据，可删除。校园导航起点。' },
    [pscustomobject]@{ name="$prefix 校园卡中心"; category='SERVICE'; building='综合服务中心'; floor='1F'; status='OPEN'; longitude=113.35502; latitude=23.12954; entranceHint='一层东侧大厅 3—6 号窗口'; description='联调测试数据，可删除。办理校园卡领取与补办。' },
    [pscustomobject]@{ name="$prefix 学院报到点"; category='SERVICE'; building='明德楼'; floor='1F'; status='OPEN'; longitude=113.35602; latitude=23.13015; entranceHint='明德楼南门进入后左转'; description='联调测试数据，可删除。新生资格核验与资料提交点。' },
    [pscustomobject]@{ name="$prefix 图书馆"; category='BUILDING'; building='图书馆'; floor='1F-6F'; status='OPEN'; longitude=113.35710; latitude=23.13074; entranceHint='主入口位于南侧，夜间请走西门'; description='联调测试数据，可删除。包含咨询台、自习区与电子阅览区。' },
    [pscustomobject]@{ name="$prefix 第一食堂"; category='LIFE'; building='第一食堂'; floor='1F-3F'; status='OPEN'; longitude=113.35775; latitude=23.12973; entranceHint='无障碍入口位于建筑东侧'; description='联调测试数据，可删除。支持校园卡和移动支付。' },
    [pscustomobject]@{ name="$prefix 学生公寓A区"; category='DORMITORY'; building='学生公寓A区'; floor='1F-12F'; status='OPEN'; longitude=113.35910; latitude=23.13030; entranceHint='A区服务台位于1号楼一层'; description='联调测试数据，可删除。新生宿舍入住办理点。' },
    [pscustomobject]@{ name="$prefix 校医院"; category='SERVICE'; building='校医院'; floor='1F-4F'; status='OPEN'; longitude=113.35838; latitude=23.12892; entranceHint='急诊入口位于北侧'; description='联调测试数据，可删除。体检与医疗服务点。' },
    [pscustomobject]@{ name="$prefix 体育馆"; category='VENUE'; building='体育馆'; floor='1F-3F'; status='MAINTENANCE'; longitude=113.36005; latitude=23.12926; entranceHint='当前东侧入口维护，请走北门'; description='联调测试数据，可删除。当前部分区域维护。' }
)

$roads = @(
    [pscustomobject]@{ name="$prefix 东门迎新通道"; fromNode="$prefix 东门"; toNode="$prefix 校园卡中心"; lengthMeters=420; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 服务中心连廊"; fromNode="$prefix 校园卡中心"; toNode="$prefix 学院报到点"; lengthMeters=360; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 明德路"; fromNode="$prefix 学院报到点"; toNode="$prefix 图书馆"; lengthMeters=580; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 湖畔步道"; fromNode="$prefix 图书馆"; toNode="$prefix 第一食堂"; lengthMeters=460; status='RESTRICTED'; restrictionReason='施工围挡，建议绕行梧桐路'; accessible=$false },
    [pscustomobject]@{ name="$prefix 梧桐路"; fromNode="$prefix 图书馆"; toNode="$prefix 学生公寓A区"; lengthMeters=920; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 生活区支路"; fromNode="$prefix 第一食堂"; toNode="$prefix 学生公寓A区"; lengthMeters=510; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 健康路"; fromNode="$prefix 第一食堂"; toNode="$prefix 校医院"; lengthMeters=680; status='OPEN'; restrictionReason=''; accessible=$true },
    [pscustomobject]@{ name="$prefix 体育馆东路"; fromNode="$prefix 校医院"; toNode="$prefix 体育馆"; lengthMeters=740; status='CLOSED'; restrictionReason='路面维护，预计三日后开放'; accessible=$false },
    [pscustomobject]@{ name="$prefix 公寓北路"; fromNode="$prefix 学生公寓A区"; toNode="$prefix 体育馆"; lengthMeters=630; status='OPEN'; restrictionReason=''; accessible=$true }
)

$tasks = @(
    [pscustomobject]@{ name="$prefix 学院报到"; description='完成身份核验并提交录取材料'; location="$prefix 学院报到点"; status='ACTIVE'; progress=89; completedCount=427; totalCount=480; estimatedMinutes=20; sortOrder=1 },
    [pscustomobject]@{ name="$prefix 领取校园卡"; description='核对信息后领取校园卡并完成首次激活'; location="$prefix 校园卡中心"; status='ACTIVE'; progress=82; completedCount=394; totalCount=480; estimatedMinutes=15; sortOrder=2 },
    [pscustomobject]@{ name="$prefix 宿舍入住"; description='在公寓服务台登记并领取钥匙'; location="$prefix 学生公寓A区"; status='ACTIVE'; progress=74; completedCount=355; totalCount=480; estimatedMinutes=25; sortOrder=3 },
    [pscustomobject]@{ name="$prefix 校园导览"; description='完成关键地点打卡并熟悉校园服务设施'; location="$prefix 东门"; status='ACTIVE'; progress=65; completedCount=312; totalCount=480; estimatedMinutes=45; sortOrder=4 }
)

$knowledge = @(
    [pscustomobject]@{ title="$prefix 2026级新生报到流程"; category='新生报到'; content='依次完成学院报到、校园卡领取、宿舍入住和校园导览。请携带录取通知书与身份证件。'; status='PUBLISHED'; source='学生工作处联调资料'; expiresAt='2026-12-31' },
    [pscustomobject]@{ title="$prefix 校园卡领取须知"; category='校园服务'; content='新生完成身份核验后，可前往校园卡中心一层东侧大厅领取并激活校园卡。'; status='PUBLISHED'; source='信息化中心联调资料'; expiresAt='2027-08-31' },
    [pscustomobject]@{ title="$prefix 宿舍入住材料清单"; category='宿舍服务'; content='办理入住需出示录取通知书、身份证和已完成报到的电子凭证。'; status='PUBLISHED'; source='后勤管理处联调资料'; expiresAt='2027-08-31' },
    [pscustomobject]@{ title="$prefix 图书馆开放说明"; category='校园设施'; content='图书馆主入口开放，湖畔步道施工期间建议从明德路或梧桐路通行。'; status='PUBLISHED'; source='图书馆联调资料'; expiresAt='2026-11-10' },
    [pscustomobject]@{ title="$prefix 体育馆临时维护通知"; category='临时通知'; content='体育馆东侧入口与体育馆东路处于维护状态，请从公寓北路前往北门。'; status='PENDING'; source='场馆中心联调资料'; expiresAt='2026-11-05' }
)

$syncedPlaces = @(Sync-Records -Endpoint '/api/places' -Key 'name' -Records $places)
$placeIds = @{}
$syncedPlaces | ForEach-Object { $placeIds[$_.name] = $_.id }
$syncedRoads = @(Sync-Records -Endpoint '/api/roads' -Key 'name' -Records $roads)
$syncedTasks = @(Sync-Records -Endpoint '/api/tasks' -Key 'name' -Records $tasks)
$syncedKnowledge = @(Sync-Records -Endpoint '/api/knowledge' -Key 'title' -Records $knowledge)

$media = @(
    [pscustomobject]@{ title="$prefix 东门入口素材记录"; placeId=$placeIds["$prefix 东门"]; placeName="$prefix 东门"; type='PHOTO'; url=''; qualityScore=92; reviewStatus='PASSED'; issueType=$null },
    [pscustomobject]@{ title="$prefix 校园卡中心素材记录"; placeId=$placeIds["$prefix 校园卡中心"]; placeName="$prefix 校园卡中心"; type='PANORAMA'; url=''; qualityScore=88; reviewStatus='PASSED'; issueType=$null },
    [pscustomobject]@{ title="$prefix 图书馆入口素材记录"; placeId=$placeIds["$prefix 图书馆"]; placeName="$prefix 图书馆"; type='PHOTO'; url=''; qualityScore=76; reviewStatus='PENDING'; issueType='LOW_QUALITY' },
    [pscustomobject]@{ title="$prefix 学生公寓导向素材记录"; placeId=$placeIds["$prefix 学生公寓A区"]; placeName="$prefix 学生公寓A区"; type='PHOTO'; url=''; qualityScore=81; reviewStatus='PENDING'; issueType='WRONG_DIRECTION' }
)
$syncedMedia = @(Sync-Records -Endpoint '/api/media' -Key 'title' -Records $media)

if ($GenerateAiActivity) {
    @(
        "$prefix 图书馆怎么去，并检查湖畔步道施工影响",
        "$prefix 学院报到任务需要在哪里办理",
        "$prefix 体育馆当前是否可以正常到达"
    ) | ForEach-Object {
        Invoke-JsonRequest -Method Post -Uri "$BaseUrl/api/ai/analyze" -Body @{ prompt=$_ } | Out-Null
    }
}

$overview = Invoke-JsonRequest -Method Get -Uri "$BaseUrl/api/dashboard/overview"
$mediaSummary = Invoke-JsonRequest -Method Get -Uri "$BaseUrl/api/media/summary"

[pscustomobject]@{
    places = $syncedPlaces.Count
    roads = $syncedRoads.Count
    tasks = $syncedTasks.Count
    knowledge = $syncedKnowledge.Count
    media = $syncedMedia.Count
    dashboardPlaces = $overview.placeCount
    dashboardRoads = $overview.roadCount
    dashboardConsultationsToday = $overview.consultationsToday
    mediaPending = $mediaSummary.pending
    mediaPassRate = $mediaSummary.passRate
} | ConvertTo-Json
