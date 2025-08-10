package org.jkiss.dbeaver.ext.mysql.auth;

import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.meta.Property;

/*
 * MySQL IAM authentication credentials.
 *
 */

public class MySQLIAMCredentials extends AuthModelDatabaseNativeCredentials {
    public static final String PROP_AWS_REGION = "awsRegion";
    public static final String PROP_AWS_PROFILE = "awsProfile";
    public static final String PROP_IAM_ROLE = "iamRole";
    public static final String PROP_SOURCE_IDENTITY = "sourceIdentity";

    private String awsRegion;
    private String awsProfile;
    private String iamRole;
    private String sourceIdentity;

    @Property(order = 10, category = "AWS")
    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @Property(order = 11, category = "AWS")
    public String getAwsProfile() {
        return awsProfile;
    }

    public void setAwsProfile(String awsProfile) {
        this.awsProfile = awsProfile;
    }

    @Property(order = 12, category = "AWS")
    public String getIamRole() {
        return iamRole;
    }

    public void setIamRole(String iamRole) {
        this.iamRole = iamRole;
    }

    @Property(order = 13, category = "AWS")
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    public void setSourceIdentity(String sourceIdentity) {
        this.sourceIdentity = sourceIdentity;
    }

    @Override
    public String toString() {
        return "MySQLIAMCredentials{" +
            "awsRegion='" + awsRegion + '\'' +
            ", awsProfile='" + awsProfile + '\'' +
            ", iamRole='" + iamRole + '\'' +
            ", sourceIdentity='" + sourceIdentity + '\'' +
            '}';
    }
}
