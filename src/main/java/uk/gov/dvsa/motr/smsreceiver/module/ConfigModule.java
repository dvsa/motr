package uk.gov.dvsa.motr.smsreceiver.module;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.apache.log4j.Logger;

import uk.gov.dvsa.motr.smsreceiver.config.CachedConfig;
import uk.gov.dvsa.motr.smsreceiver.config.Config;
import uk.gov.dvsa.motr.smsreceiver.config.ConfigKey;
import uk.gov.dvsa.motr.smsreceiver.config.EncryptionAwareConfig;
import uk.gov.dvsa.motr.smsreceiver.config.EnvironmentVariableConfig;
import uk.gov.dvsa.motr.smsreceiver.encryption.AwsKmsDecryptor;
import uk.gov.dvsa.motr.smsreceiver.encryption.Decryptor;
import uk.gov.dvsa.motr.smsreceiver.service.CancelledSubscriptionHelper;
import uk.gov.dvsa.motr.smsreceiver.service.MessageExtractor;
import uk.gov.dvsa.motr.smsreceiver.service.SmsMessageValidator;
import uk.gov.dvsa.motr.smsreceiver.service.VrmValidator;
import uk.gov.dvsa.motr.smsreceiver.subscription.persistence.CancelledSubscriptionRepository;
import uk.gov.dvsa.motr.smsreceiver.subscription.persistence.DynamoDbCancelledSubscriptionRepository;
import uk.gov.dvsa.motr.smsreceiver.subscription.persistence.DynamoDbSubscriptionRepository;
import uk.gov.dvsa.motr.smsreceiver.subscription.persistence.SubscriptionRepository;

import java.util.HashSet;
import java.util.Set;

import static com.amazonaws.regions.Region.getRegion;
import static com.amazonaws.regions.Regions.fromName;

import static org.apache.log4j.Level.toLevel;

import static uk.gov.dvsa.motr.smsreceiver.system.SystemVariable.DB_TABLE_CANCELLED_SUBSCRIPTION;
import static uk.gov.dvsa.motr.smsreceiver.system.SystemVariable.DB_TABLE_SUBSCRIPTION;
import static uk.gov.dvsa.motr.smsreceiver.system.SystemVariable.LOG_LEVEL;
import static uk.gov.dvsa.motr.smsreceiver.system.SystemVariable.NOTIFY_BEARER_TOKEN;
import static uk.gov.dvsa.motr.smsreceiver.system.SystemVariable.REGION;

public class ConfigModule extends AbstractModule {

    @Override
    protected void configure() {

        Config config = new CachedConfig(new EnvironmentVariableConfig());
        String region = config.getValue(REGION);
        Decryptor decryptor =  new AwsKmsDecryptor(getRegion(fromName(region)));

        config = new CachedConfig(
                new EncryptionAwareConfig(
                        config,
                        secretVariables(),
                        decryptor
                )
        );

        bind(Config.class).toInstance(config);
        Logger.getRootLogger().setLevel(toLevel(config.getValue(LOG_LEVEL)));
    }

    @Provides
    public SubscriptionRepository provideSubscriptionRepository(Config config) {

        return new DynamoDbSubscriptionRepository(config.getValue(DB_TABLE_SUBSCRIPTION), config.getValue(REGION));
    }

    @Provides
    public SmsMessageValidator provideSmsMessageHelper(MessageExtractor messageExtractor) {

        return new SmsMessageValidator();
    }

    @Provides
    public CancelledSubscriptionRepository provideCancelledSubscriptionRepository(Config config) {

        return new DynamoDbCancelledSubscriptionRepository(config.getValue(DB_TABLE_CANCELLED_SUBSCRIPTION), config.getValue(REGION));
    }

    @Provides
    public CancelledSubscriptionHelper provideCancelledSubscriptionHelper(CancelledSubscriptionRepository cancelledSubscriptionRepository) {

        return new CancelledSubscriptionHelper(cancelledSubscriptionRepository);
    }

    @Provides
    public VrmValidator provideVrmValidator() {

        return new VrmValidator();
    }

    @Provides
    public MessageExtractor provideMessaherExtractor() {

        return new MessageExtractor();
    }

    @Provides
    public Decryptor provideDecryptor(Config config) {

        return new AwsKmsDecryptor(getRegion(fromName(config.getValue(REGION))));
    }

    @Provides
    public String provideToken(Config config) {

        return config.getValue(NOTIFY_BEARER_TOKEN);
    }

    private static Set<ConfigKey> secretVariables() {

        Set<ConfigKey> secretVariables = new HashSet<>();
        secretVariables.add(NOTIFY_BEARER_TOKEN);
        return secretVariables;
    }
}
