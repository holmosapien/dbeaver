package org.jkiss.dbeaver.ext.mysql.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.awssdk.services.rds.RdsUtilities;

import java.util.Properties;

/*
 * MySQL IAM authentication model for RDS.
 *
 */

public class MySQLIAMAuthModel extends AuthModelDatabaseNative<MySQLIAMCredentials> {
    private static final Log log = Log.getLog(MySQLIAMAuthModel.class);

    public static final String ID = "mysql_iam";

    @NotNull
    @Override
    public MySQLIAMCredentials createCredentials() {
        return new MySQLIAMCredentials();
    }

    @NotNull
    @Override
    public MySQLIAMCredentials loadCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration) {
        MySQLIAMCredentials credentials = super.loadCredentials(dataSource, configuration);

        String awsRegion = configuration.getAuthProperty(MySQLIAMCredentials.PROP_AWS_REGION);
        String awsProfile = configuration.getAuthProperty(MySQLIAMCredentials.PROP_AWS_PROFILE);
        String iamRole = configuration.getAuthProperty(MySQLIAMCredentials.PROP_IAM_ROLE);
        String sourceIdentity = configuration.getAuthProperty(MySQLIAMCredentials.PROP_SOURCE_IDENTITY);

        credentials.setAwsRegion(awsRegion);
        credentials.setAwsProfile(awsProfile);
        credentials.setIamRole(iamRole);
        credentials.setSourceIdentity(sourceIdentity);

        return credentials;
    }

    @Override
    public void saveCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull MySQLIAMCredentials credentials) {
        configuration.setAuthProperty(MySQLIAMCredentials.PROP_AWS_REGION, credentials.getAwsRegion());
        configuration.setAuthProperty(MySQLIAMCredentials.PROP_AWS_PROFILE, credentials.getAwsProfile());
        configuration.setAuthProperty(MySQLIAMCredentials.PROP_IAM_ROLE, credentials.getIamRole());
        configuration.setAuthProperty(MySQLIAMCredentials.PROP_SOURCE_IDENTITY, credentials.getSourceIdentity());

        super.saveCredentials(dataSource, configuration, credentials);
    }

    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull MySQLIAMCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connectProps
    ) throws DBException {
        log.debug("Initializing MySQL IAM authentication");

        if (shouldUseIAMAuth(configuration)) {
            log.info("IAM authentication enabled, getting RDS IAM authentication token");

            String authToken = getRDSIAMAuthToken(monitor, dataSource, credentials);

            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, authToken);
        } else {
            log.debug("IAM authentication not enabled, using standard authentication");
        }

        return super.initAuthentication(monitor, dataSource, credentials, configuration, connectProps);
    }

    private boolean shouldUseIAMAuth(DBPConnectionConfiguration configuration) {
        String iamRole = configuration.getAuthProperty(MySQLIAMCredentials.PROP_IAM_ROLE);
        boolean shouldUse = !CommonUtils.isEmpty(iamRole);

        return shouldUse;
    }

    private String getRDSIAMAuthToken(DBRProgressMonitor monitor, DBPDataSource dataSource, MySQLIAMCredentials credentials) throws DBException {
        try {
            monitor.subTask("Getting AWS IAM authentication token");

            String authToken = generateRDSIAMToken(dataSource, credentials);

            return authToken;
        } catch (Exception e) {
            log.error("Failed to get AWS IAM authentication token", e);

            throw new DBException("Failed to get AWS IAM authentication token", e);
        }
    }

    private String generateRDSIAMToken(DBPDataSource dataSource, MySQLIAMCredentials credentials) throws DBException {
        DBPDataSourceContainer container = dataSource.getContainer();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();

        String hostname = configuration.getHostName();
        String port = configuration.getHostPort();
        String username = configuration.getUserName();

        String region = credentials.getAwsRegion();
        String profile = credentials.getAwsProfile();
        String role = credentials.getIamRole();
        String sourceIdentity = credentials.getSourceIdentity();

        try {

            /*
             * Load AWS credentials from the user's profile.
             *
             */

            AwsCredentialsProvider credentialsProvider = loadAwsCredentials(profile);

            /*
             * Assume the IAM role using the STS API.
             *
             */

            Credentials assumedCredentials = assumeIamRole(credentialsProvider, region, role, sourceIdentity);

            /*
             * Generate the RDS token using the assumed credentials.
             *
             */

            String authToken = generateRDSToken(assumedCredentials, region, hostname, port, username);

            return authToken;
        } catch (Exception e) {
            log.error("Failed to generate RDS IAM authentication token", e);

            throw new DBException("Failed to generate RDS IAM authentication token", e);
        }
    }

    private static AwsCredentialsProvider loadAwsCredentials(String profile) {
        if (profile == null || profile.isEmpty() || "default".equals(profile)) {
            log.debug("Using default credentials provider chain");

            return DefaultCredentialsProvider.create();
        } else {
            log.debug("Using profile: " + profile);

            return ProfileCredentialsProvider.create(profile);
        }
    }

    private static Credentials assumeIamRole(AwsCredentialsProvider credentialsProvider, String region, String roleArn, String sourceIdentity) {
        if (roleArn == null || roleArn.isEmpty()) {
            log.debug("No IAM role specified, using original credentials");

            return null;
        }

        StsClient stsClient = StsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();

        AssumeRoleRequest.Builder requestBuilder = AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName("DBeaver-" + System.currentTimeMillis());

        if (sourceIdentity != null && !sourceIdentity.isEmpty()) {
            log.debug("Using source identity: " + sourceIdentity);

            requestBuilder.sourceIdentity(sourceIdentity);
        }

        AssumeRoleRequest request = requestBuilder.build();
        AssumeRoleResponse response = stsClient.assumeRole(request);

        log.debug("Successfully assumed IAM role " + roleArn);

        return response.credentials();
    }

    private static String generateRDSToken(Credentials credentials, String region, String hostname, String port, String username) {
        RdsClient rdsClient;

        if (credentials != null) {
            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken()
            );

            AwsCredentialsProvider sessionCredentialsProvider = () -> sessionCredentials;

            log.debug("Creating RDS client with session credentials");

            rdsClient = RdsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(sessionCredentialsProvider)
                .build();
        } else {
            log.debug("Creating RDS client with default credentials");

            rdsClient = RdsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        }

        // Generate authentication token
        GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest.builder()
            .hostname(hostname)
            .port(Integer.parseInt(port))
            .username(username)
            .build();

        RdsUtilities utilities = rdsClient.utilities();
        String token = utilities.generateAuthenticationToken(request);

        log.debug("Successfully generated RDS authentication token");

        return token;
    }
}