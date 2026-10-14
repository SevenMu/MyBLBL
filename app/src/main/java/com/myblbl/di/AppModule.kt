package com.myblbl.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.myblbl.core.common.settings.AppSettingsDataStore
import com.myblbl.event.AppEventHub
import com.myblbl.network.NetworkManager
import com.myblbl.network.api.ApiService
import com.myblbl.network.security.NetworkManagerSecurityGateway
import com.myblbl.network.security.NetworkManagerWebGateway
import com.myblbl.network.security.NetworkSecurityGateway
import com.myblbl.network.security.NetworkWebGateway
import com.myblbl.network.session.NetworkManagerSessionGateway
import com.myblbl.network.session.NetworkSessionGateway
import com.myblbl.network.session.SessionStateRepository
import com.myblbl.repository.AllSeriesRepository
import com.myblbl.repository.AuthRepository
import com.myblbl.repository.FavoriteRepository
import com.myblbl.repository.HomeLaneRepository
import com.myblbl.repository.PersonalFeedPrewarmer
import com.myblbl.repository.LiveRepository
import com.myblbl.repository.SearchRepository
import com.myblbl.repository.SeriesRepository
import com.myblbl.repository.UserRepository
import com.myblbl.repository.VideoRepository
import com.myblbl.feature.category.CategoryViewModel
import com.myblbl.feature.dynamic.DynamicViewModel
import com.myblbl.feature.home.HotViewModel
import com.myblbl.feature.home.HotFeedRepository
import com.myblbl.feature.home.HomeLaneFeedRepository
import com.myblbl.feature.home.HomeLaneViewModel
import com.myblbl.feature.home.RecommendDislikeFeedback
import com.myblbl.feature.home.RecommendFeedRepository
import com.myblbl.feature.home.RecommendViewModel
import com.myblbl.feature.home.RegionFeedViewModel
import com.myblbl.feature.live.LiveListViewModel
import com.myblbl.feature.live.LiveRecommendViewModel
import com.myblbl.feature.live.LiveViewModel
import com.myblbl.feature.me.MeListViewModel
import com.myblbl.feature.me.MeViewModel
import com.myblbl.feature.search.SearchViewModel
import com.myblbl.feature.player.LivePlayerViewModel
import com.myblbl.feature.player.danmaku.LiveDanmakuManager
import com.myblbl.feature.player.douyin.DouyinModeManager
import com.myblbl.ui.fragment.main.MainNavigationViewModel
import com.myblbl.feature.player.VideoPlayerViewModel
import com.myblbl.feature.series.SeriesDetailViewModel
import com.myblbl.network.cookie.CookieManager
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Koin DI 延迟加载检查结论：
// Koin 的 single {} 和 viewModel {} 默认都是延迟创建的（首次请求时才实例化），
// 当前模块声明方式已经是最佳实践，无需额外优化。
val networkModule = module {
    single<ApiService> { NetworkManager.apiService }
    single<OkHttpClient> { NetworkManager.getOkHttpClient() }
    single<CookieManager> { NetworkManager.getCookieManager() }
    // 同一实例绑定双接口：Repository/播放内核用 NetworkSessionGateway，UI 层只注入 SessionStateRepository
    single<NetworkSessionGateway> { NetworkManagerSessionGateway() }
    single<SessionStateRepository> { get<NetworkSessionGateway>() as SessionStateRepository }
    single<NetworkSecurityGateway> { NetworkManagerSecurityGateway() }
    single<NetworkWebGateway> { NetworkManagerWebGateway() }
    factory(named("noCookie")) { NetworkManager.noCookieApiService }
}

val repositoryModule = module {
    single { com.myblbl.repository.remote.AllSeriesRepository(get()) }
    single { com.myblbl.repository.remote.AuthRepository(get()) }
    single { com.myblbl.repository.remote.TvAuthRepository(get(named("noCookie"))) }
    single { com.myblbl.repository.remote.FavoriteRepository(get(), get(), get()) }
    single { com.myblbl.repository.remote.HomeLaneRepository(get(), get(), get(), get()) }
    single { com.myblbl.repository.remote.LiveRepository(get(), get()) }
    single { com.myblbl.repository.remote.SearchRepository(get(), get()) }
    single { com.myblbl.repository.remote.SeriesRepository(get(), get(), get()) }
    single { com.myblbl.repository.remote.VideoRepository(get(), get(), get()) }
    single { AllSeriesRepository(get()) }
    single { AuthRepository(get()) }
    single { FavoriteRepository(get()) }
    single { HomeLaneRepository(get()) }
    single { LiveRepository(get()) }
    single { SearchRepository(get()) }
    single { SeriesRepository(get()) }
    single { VideoRepository(get(), get()) }
    single { UserRepository(get(), get(), get()) }
    single { PersonalFeedPrewarmer(get(), get(), get()) }
    single { RecommendFeedRepository(androidContext()) }
    single { HotFeedRepository() }
    single { HomeLaneFeedRepository(get()) }
}

@OptIn(UnstableApi::class)
val viewModelModule = module {
    single { RecommendDislikeFeedback(get(), get()) }
    viewModel { RecommendViewModel(get(), get(), androidContext()) }
    viewModel { HotViewModel(get(), get(), androidContext()) }
    viewModel { (type: Int) -> HomeLaneViewModel(type, get()) }
    viewModel { (rid: Int) -> RegionFeedViewModel(rid, get(), androidContext()) }
    viewModel { MainNavigationViewModel(get()) }
    viewModel { VideoPlayerViewModel(get(), get(), get(), get(), get(), get(), get(named("noCookie")), get(), androidContext(), get()) }
    viewModel { CategoryViewModel(get()) }
    viewModel { DynamicViewModel(get()) }
    viewModel { LiveViewModel(get(), androidContext()) }
    viewModel { LiveListViewModel(get()) }
    viewModel { LiveRecommendViewModel(get()) }
    viewModel { MeListViewModel(get(), get()) }
    viewModel { MeViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { LivePlayerViewModel(get(), LiveDanmakuManager(get(), get(), get()), androidContext()) }
    viewModel { SeriesDetailViewModel(get(), get()) }
}

val eventModule = module {
    single { AppEventHub() }
    single { AppSettingsDataStore(androidContext()) }
    single { DouyinModeManager(androidContext(), get(), get(), get(named("noCookie")), get()) }
}

val appModules = listOf(
    networkModule,
    repositoryModule,
    eventModule,
    viewModelModule
)
