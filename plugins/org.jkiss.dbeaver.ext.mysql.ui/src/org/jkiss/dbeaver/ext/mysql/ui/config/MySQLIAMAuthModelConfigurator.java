package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.auth.MySQLIAMCredentials;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

/*
 * MySQL IAM Authentication Model Configurator
 *
 */

public class MySQLIAMAuthModelConfigurator extends DatabaseNativeAuthModelConfigurator implements IObjectPropertyConfigurator<DBAAuthModel<?>, DBPDataSourceContainer> {

    private Combo awsRegionCombo;
    private Text awsProfileText;
    private Text iamRoleText;
    private Text sourceIdentityText;

    @Override
    public void createControl(@NotNull Composite authPanel, DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        // First, let the parent create the standard controls (username, etc.)
        super.createControl(authPanel, object, propertyChangeListener);

        // AWS Region
        UIUtils.createControlLabel(authPanel, "AWS Region");
        awsRegionCombo = new Combo(authPanel, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        awsRegionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        awsRegionCombo.addModifyListener(e -> propertyChangeListener.run());

        // Populate AWS regions
        populateAWSRegions();

        // AWS Profile
        UIUtils.createControlLabel(authPanel, "AWS Profile");
        awsProfileText = new Text(authPanel, SWT.BORDER);
        awsProfileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        awsProfileText.setToolTipText("AWS profile name (leave empty for default)");
        awsProfileText.addModifyListener(e -> propertyChangeListener.run());

        // IAM Role ARN
        UIUtils.createControlLabel(authPanel, "IAM Role ARN");
        iamRoleText = new Text(authPanel, SWT.BORDER);
        iamRoleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        iamRoleText.setToolTipText("ARN of the IAM role to assume (e.g., arn:aws:iam::123456789012:role/MyRole)");
        iamRoleText.addModifyListener(e -> propertyChangeListener.run());

        // Source Identity (optional)
        UIUtils.createControlLabel(authPanel, "Source Identity (Optional)");
        sourceIdentityText = new Text(authPanel, SWT.BORDER);
        sourceIdentityText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sourceIdentityText.setToolTipText("Optional source identity for the assumed role session");
        sourceIdentityText.addModifyListener(e -> propertyChangeListener.run());
    }

    private void populateAWSRegions() {
        String[] regions = {
            "us-east-1",
            "us-east-2",
            "us-west-1",
            "us-west-2",
            "us-gov-east-1",
            "us-gov-west-1",
            "ca-central-1",
            "eu-central-1",
            "eu-west-1",
            "eu-west-2",
            "eu-west-3",
            "ap-northeast-1",
            "ap-northeast-2",
            "ap-southeast-1",
            "ap-southeast-2",
            "ap-south-1",
            "sa-east-1"
        };

        for (String region : regions) {
            awsRegionCombo.add(region);
        }

        // Set default region
        awsRegionCombo.setText("us-east-1");
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        // Load AWS Region
        String awsRegion = connectionInfo.getAuthProperty(MySQLIAMCredentials.PROP_AWS_REGION);
        if (awsRegion != null && awsRegionCombo != null) {
            awsRegionCombo.setText(awsRegion);
        }

        // Load AWS Profile
        String awsProfile = connectionInfo.getAuthProperty(MySQLIAMCredentials.PROP_AWS_PROFILE);
        if (awsProfile != null && awsProfileText != null) {
            awsProfileText.setText(awsProfile);
        }

        // Load IAM Role
        String iamRole = connectionInfo.getAuthProperty(MySQLIAMCredentials.PROP_IAM_ROLE);
        if (iamRole != null && iamRoleText != null) {
            iamRoleText.setText(iamRole);
        }

        // Load Source Identity
        String sourceIdentity = connectionInfo.getAuthProperty(MySQLIAMCredentials.PROP_SOURCE_IDENTITY);
        if (sourceIdentity != null && sourceIdentityText != null) {
            sourceIdentityText.setText(sourceIdentity);
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        // Save AWS Region
        if (awsRegionCombo != null) {
            connectionInfo.setAuthProperty(MySQLIAMCredentials.PROP_AWS_REGION, awsRegionCombo.getText().trim());
        }

        // Save AWS Profile
        if (awsProfileText != null) {
            connectionInfo.setAuthProperty(MySQLIAMCredentials.PROP_AWS_PROFILE, awsProfileText.getText().trim());
        }

        // Save IAM Role
        if (iamRoleText != null) {
            connectionInfo.setAuthProperty(MySQLIAMCredentials.PROP_IAM_ROLE, iamRoleText.getText().trim());
        }

        // Save Source Identity
        if (sourceIdentityText != null) {
            connectionInfo.setAuthProperty(MySQLIAMCredentials.PROP_SOURCE_IDENTITY, sourceIdentityText.getText().trim());
        }
    }

    @Override
    public boolean isComplete() {
        // Check if required fields are filled
        boolean hasRegion = awsRegionCombo != null && !CommonUtils.isEmpty(awsRegionCombo.getText());
        boolean hasRole = iamRoleText != null && !CommonUtils.isEmpty(iamRoleText.getText());

        return hasRegion && hasRole && super.isComplete();
    }
}