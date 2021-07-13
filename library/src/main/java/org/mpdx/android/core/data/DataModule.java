package org.mpdx.android.core.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.moshi.Moshi;

import org.ccci.gto.android.common.api.okhttp3.interceptor.SessionRetryInterceptor;
import org.ccci.gto.android.common.api.retrofit2.converter.LocaleConverterFactory;
import org.ccci.gto.android.common.jsonapi.JsonApiConverter;
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiConverterFactory;
import org.ccci.gto.android.common.okhttp3.util.OkHttpClientUtil;
import org.mpdx.android.base.AppConstantListener;
import org.mpdx.android.base.AuthenticationListener;
import org.mpdx.android.core.AuthApi;
import org.mpdx.android.core.api.converter.DateTypeConverter;
import org.mpdx.android.core.api.interceptor.MpdxSessionInterceptor;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.appeals.model.Appeal;
import org.mpdx.android.features.appeals.model.AskedContact;
import org.mpdx.android.features.appeals.model.ExcludedAppealContact;
import org.mpdx.android.features.appeals.model.Pledge;
import org.mpdx.android.features.coaching.model.CoachingAccountList;
import org.mpdx.android.features.coaching.model.CoachingAnalytics;
import org.mpdx.android.features.coaching.model.CoachingAppointmentResults;
import org.mpdx.android.features.constants.api.converter.CruHashesConverter;
import org.mpdx.android.features.constants.model.ConstantList;
import org.mpdx.android.features.contacts.api.converter.UserPreferencesConverter;
import org.mpdx.android.features.contacts.model.Address;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.model.DonorAccount;
import org.mpdx.android.features.contacts.model.EmailAddress;
import org.mpdx.android.features.contacts.model.FacebookAccount;
import org.mpdx.android.features.contacts.model.LinkedInAccount;
import org.mpdx.android.features.contacts.model.Person;
import org.mpdx.android.features.contacts.model.PhoneNumber;
import org.mpdx.android.features.contacts.model.TwitterAccount;
import org.mpdx.android.features.contacts.model.Website;
import org.mpdx.android.features.dashboard.model.GoalProgress;
import org.mpdx.android.features.donations.model.DesignationAccount;
import org.mpdx.android.features.donations.model.Donation;
import org.mpdx.android.features.notifications.model.Notification;
import org.mpdx.android.features.notifications.model.NotificationType;
import org.mpdx.android.features.notifications.model.UserNotification;
import org.mpdx.android.features.settings.model.NotificationPreference;
import org.mpdx.android.features.tasks.api.converter.OverdueTaskConverter;
import org.mpdx.android.core.api.interceptor.MpdxRequestInterceptor;
import org.mpdx.android.core.data.typeadapter.CRUCurrencyDeserializer;
import org.mpdx.android.core.data.typeadapter.CRUListDeserializer;
import org.mpdx.android.core.data.typeadapter.LocaleDeserializer;
import org.mpdx.android.core.data.typeadapter.MapWrapperDeserializer;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.core.model.Authenticate;
import org.mpdx.android.core.model.DeletedRecord;
import org.mpdx.android.core.model.Tag;
import org.mpdx.android.core.model.User;
import org.mpdx.android.core.model.UserDevice;
import org.mpdx.android.features.tasks.model.Comment;
import org.mpdx.android.features.tasks.model.Task;
import org.mpdx.android.features.tasks.model.TaskAnalytics;
import org.mpdx.android.features.tasks.model.TaskContact;


import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.Multibinds;
import io.realm.Realm;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@InstallIn(SingletonComponent.class)
@Module
public abstract class DataModule {
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    @Provides
    @Singleton
    static Gson provideGson() {
        return new GsonBuilder()
                .create();
    }

    @Provides
    @Singleton
    static Moshi provideMoshi() {
        return new Moshi.Builder()
                .build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(@Named("baseRetrofit") Retrofit retrofit, OkHttpClient okhttp, Gson gson) {
        return retrofit.newBuilder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okhttp)
                .build();
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(@Named("baseOkHttp") OkHttpClient okhttp, Context context, AuthenticationListener authenticationListener,
                                            AuthApi authApi) {
        final OkHttpClient.Builder builder = okhttp.newBuilder();
        builder.networkInterceptors().add(0, new MpdxSessionInterceptor(context, authenticationListener, authApi));
        builder.addInterceptor(new SessionRetryInterceptor());
        return builder.build();
    }

    @Provides
    @Singleton
    @Named("baseRetrofit")
    static Retrofit provideBaseRetrofit(@Named("baseOkHttp") OkHttpClient okHttpClient,
                                        JsonApiConverter jsonApiConverter, AppConstantListener listener) {
        return new Retrofit.Builder()
                .baseUrl(listener.baseApiUrl())
                .validateEagerly(listener.isDebug())
                .addConverterFactory(JsonApiConverterFactory.create(jsonApiConverter))
                .addConverterFactory(new LocaleConverterFactory())
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    @Named("baseOkHttp")
    static OkHttpClient provideBaseOkHttpClient(AppPrefs appPrefs, Set<Interceptor> networkInterceptors) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addNetworkInterceptor(new MpdxRequestInterceptor(appPrefs));

        for (final Interceptor interceptor : networkInterceptors) {
            builder.addNetworkInterceptor(interceptor);
        }

        OkHttpClientUtil.attachGlobalInterceptors(builder);
        return builder
                .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Multibinds
    abstract Set<Interceptor> networkInterceptors();

    @Provides
    @Singleton
    static Realm provideRealm() {
        return Realm.getDefaultInstance();
    }

    @Singleton
    @Provides
    static JsonApiConverter jsonApiConverter(OverdueTaskConverter overdueTaskConverter,
                                             UserPreferencesConverter userPreferencesConverter,
                                             MapWrapperDeserializer mapWrapperDeserializer,
                                             LocaleDeserializer localeDeserializer,
                                             CRUCurrencyDeserializer currencyDeserializer,
                                             CRUListDeserializer listDeserializer,
                                             CruHashesConverter cruHashesConverter) {
        return new JsonApiConverter.Builder()
                .addClasses(Authenticate.class)
                // feature/constants
                .addClasses(ConstantList.class)
                .addConverters(cruHashesConverter)
                // features/tasks
                .addClasses(Task.class, Comment.class, TaskContact.class)
                .addClasses(AccountList.class, User.class, Person.class, Donation.class, GoalProgress.class,
                        DeletedRecord.class, CoachingAccountList.class, Tag.class, UserDevice.class,
                        CoachingAnalytics.class)
                .addClasses(Notification.class, NotificationType.class, NotificationPreference.class,
                        UserNotification.class)
                .addClasses(Contact.class, Address.class, Appeal.class, Pledge.class, AskedContact.class,
                        DonorAccount.class, DesignationAccount.class, ExcludedAppealContact.class,
                        CoachingAppointmentResults.class, TaskAnalytics.class,
                        PhoneNumber.class, EmailAddress.class)
                .addClasses(FacebookAccount.class, LinkedInAccount.class, TwitterAccount.class, Website.class)
                .addConverters(DateTypeConverter.INSTANCE, overdueTaskConverter, userPreferencesConverter,
                        mapWrapperDeserializer, localeDeserializer, listDeserializer, currencyDeserializer)
                .build();
    }
}
