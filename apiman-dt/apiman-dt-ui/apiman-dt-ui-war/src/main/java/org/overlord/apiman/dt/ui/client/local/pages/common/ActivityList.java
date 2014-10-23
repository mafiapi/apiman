/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.apiman.dt.ui.client.local.pages.common;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.nav.client.local.TransitionAnchor;
import org.jboss.errai.ui.nav.client.local.TransitionAnchorFactory;
import org.overlord.apiman.dt.api.beans.audit.AuditEntityType;
import org.overlord.apiman.dt.api.beans.audit.AuditEntryBean;
import org.overlord.apiman.dt.api.beans.audit.AuditEntryType;
import org.overlord.apiman.dt.api.beans.search.SearchResultsBean;
import org.overlord.apiman.dt.ui.client.local.AppMessages;
import org.overlord.apiman.dt.ui.client.local.events.MoreActivityItemsEvent;
import org.overlord.apiman.dt.ui.client.local.events.MoreActivityItemsEvent.Handler;
import org.overlord.apiman.dt.ui.client.local.events.MoreActivityItemsEvent.HasMoreActivityItemsHandlers;
import org.overlord.apiman.dt.ui.client.local.pages.AppRedirectPage;
import org.overlord.apiman.dt.ui.client.local.pages.OrgRedirectPage;
import org.overlord.apiman.dt.ui.client.local.pages.PlanRedirectPage;
import org.overlord.apiman.dt.ui.client.local.pages.ServiceRedirectPage;
import org.overlord.apiman.dt.ui.client.local.pages.UserRedirectPage;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.AbstractDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.AddPolicyDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.GrantDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.RemovePolicyDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.RevokeDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.UpdateDetailPanel;
import org.overlord.apiman.dt.ui.client.local.pages.common.activity.UpdatePolicyDetailPanel;
import org.overlord.apiman.dt.ui.client.local.util.MultimapUtil;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;
import org.overlord.commons.gwt.client.local.widgets.FontAwesomeIcon;

import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Widget used to show activity/audit information in all of the relevant
 * entity UI pages.
 *
 * @author eric.wittmann@redhat.com
 */
@Dependent
public class ActivityList extends FlowPanel implements TakesValue<SearchResultsBean<AuditEntryBean>>, HasMoreActivityItemsHandlers {
    
    @Inject
    protected TranslationService i18n;
    
    @Inject
    TransitionAnchorFactory<UserRedirectPage> userLinkFactory;
    @Inject
    TransitionAnchorFactory<OrgRedirectPage> orgLinkFactory;
    @Inject
    TransitionAnchorFactory<PlanRedirectPage> planLinkFactory;
    @Inject
    TransitionAnchorFactory<AppRedirectPage> appLinkFactory;
    @Inject
    TransitionAnchorFactory<ServiceRedirectPage> serviceLinkFactory;
    
    @Inject
    Instance<GrantDetailPanel> grantPanelFactory;
    @Inject
    Instance<RevokeDetailPanel> revokePanelFactory;
    @Inject
    Instance<UpdatePolicyDetailPanel> updatePolicyPanelFactory;
    @Inject
    Instance<AddPolicyDetailPanel> addPolicyPanelFactory;
    @Inject
    Instance<RemovePolicyDetailPanel> removePolicyPanelFactory;
    @Inject
    Instance<UpdateDetailPanel> updatePanelFactory;
    
    private int count;
    private AsyncActionButton moreItemsButton;

    /**
     * Constructor.
     */
    public ActivityList() {
    }

    /**
     * @see com.google.gwt.user.client.TakesValue#setValue(java.lang.Object)
     */
    @Override
    public void setValue(SearchResultsBean<AuditEntryBean> value) {
        clear();
        count = 0;
        appendValue(value);
    }

    /**
     * Appends more items to the list.
     * @param value
     */
    public void appendValue(SearchResultsBean<AuditEntryBean> value) {
        if (moreItemsButton != null) {
            moreItemsButton.onActionComplete();
            remove(moreItemsButton.getParent());
        }

        List<AuditEntryBean> beans = value.getBeans();
        for (AuditEntryBean auditEntryBean : beans) {
            addRow(auditEntryBean);
            count++;
        }

        if (value.getTotalSize() > count) {
            if (moreItemsButton == null) {
                FlowPanel container = new FlowPanel();
                container.getElement().setClassName("container-fluid"); //$NON-NLS-1$
                container.getElement().getStyle().setTextAlign(TextAlign.CENTER);
                moreItemsButton = new AsyncActionButton();
                moreItemsButton.setText(i18n.format(AppMessages.SHOW_NEXT_ITEMS));
                moreItemsButton.setActionText(i18n.format(AppMessages.LOADING_ITEMS));
                moreItemsButton.setIcon("fa-cog"); //$NON-NLS-1$
                moreItemsButton.getElement().setClassName("btn"); //$NON-NLS-1$
                moreItemsButton.getElement().addClassName("btn-default"); //$NON-NLS-1$
                moreItemsButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        moreItemsButton.onActionStarted();
                        MoreActivityItemsEvent.fire(ActivityList.this);
                    }
                });
                container.add(moreItemsButton);
                add(container);
            } else {
                add(moreItemsButton.getParent());
            }
        }
    }

    /**
     * Creates a single row in the list of audit/activity information.
     * 
     * @param auditEntryBean
     */
    private void addRow(AuditEntryBean auditEntryBean) {
        FlowPanel container = new FlowPanel();
        container.getElement().setClassName("container-fluid"); //$NON-NLS-1$
        container.getElement().addClassName("apiman-summaryrow"); //$NON-NLS-1$
        
        FlowPanel row1 = new FlowPanel();
        row1.getElement().setClassName("row"); //$NON-NLS-1$
        container.add(row1);
        
        TransitionAnchor<UserRedirectPage> userLink = userLinkFactory.get("user", auditEntryBean.getWho()); //$NON-NLS-1$
        userLink.setText(auditEntryBean.getWho());
        row1.add(userLink);
        
        row1.add(new InlineLabel(" ")); //$NON-NLS-1$
        
        String whatString = createWhat(auditEntryBean);
        row1.add(new InlineLabel(whatString));

        row1.add(new InlineLabel(" ")); //$NON-NLS-1$

        TransitionAnchor<OrgRedirectPage> orgLink = orgLinkFactory.get("org", auditEntryBean.getOrganizationId()); //$NON-NLS-1$
        orgLink.setText(auditEntryBean.getOrganizationId());
        row1.add(orgLink);
        
        if (auditEntryBean.getEntityId() != null) {
            row1.add(new InlineLabel(" / ")); //$NON-NLS-1$
            row1.add(createEntityLink(auditEntryBean));
            
            if (auditEntryBean.getEntityVersion() != null) {
                row1.add(new InlineLabel(" ")); //$NON-NLS-1$
                row1.add(new InlineLabel(i18n.format(AppMessages.VERSION)));
                row1.add(new InlineLabel(" ")); //$NON-NLS-1$
                row1.add(createEntityVersionLink(auditEntryBean));
            }
            
        } else {
            orgLink.getElement().setClassName("emphasis"); //$NON-NLS-1$
        }
        row1.add(new InlineLabel(".")); //$NON-NLS-1$
        
        Widget detailRow = createDetailRow(auditEntryBean);
        if (detailRow != null) {
            container.add(detailRow);
        }
        
        FlowPanel row2 = new FlowPanel();
        row2.getElement().setClassName("row"); //$NON-NLS-1$
        container.add(row2);
        
        String icon = getIcon(auditEntryBean);
        if (icon != null) {
            row2.add(new FontAwesomeIcon(icon, true));
        }
        
        row2.add(createWhen(auditEntryBean));
        
        container.add(new HTMLPanel("<hr/>")); //$NON-NLS-1$
        
        add(container);
    }

    /**
     * Creates a link to the entity.
     * @param auditEntryBean
     */
    private Widget createEntityLink(AuditEntryBean auditEntryBean) {
        Anchor rval = null;
        switch (auditEntryBean.getEntityType()) {
        case Application:
            rval = appLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "app", auditEntryBean.getEntityId())); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        case Plan:
            rval = planLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "plan", auditEntryBean.getEntityId())); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        case Service:
            rval = serviceLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "service", auditEntryBean.getEntityId())); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        default:
            break;
        }
        if (rval == null) {
            return new InlineLabel();
        }
        rval.setText(auditEntryBean.getEntityId());
        rval.getElement().setClassName("emphasis"); //$NON-NLS-1$
        return rval;
    }

    /**
     * Creates a link to the entity version.
     * @param auditEntryBean
     */
    private Widget createEntityVersionLink(AuditEntryBean auditEntryBean) {
        Anchor rval = null;
        switch (auditEntryBean.getEntityType()) {
        case Application:
            rval = appLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "app", auditEntryBean.getEntityId(), "version", auditEntryBean.getEntityVersion())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            break;
        case Plan:
            rval = planLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "plan", auditEntryBean.getEntityId(), "version", auditEntryBean.getEntityVersion())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            break;
        case Service:
            rval = serviceLinkFactory.get(MultimapUtil.fromMultiple("org", auditEntryBean.getOrganizationId(), "service", auditEntryBean.getEntityId(), "version", auditEntryBean.getEntityVersion())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            break;
        default:
            break;
        }
        if (rval == null) {
            return new InlineLabel();
        }
        rval.setText(auditEntryBean.getEntityVersion());
        rval.getElement().setClassName("emphasis"); //$NON-NLS-1$
        return rval;
    }

    /**
     * @param auditEntryBean
     * @return
     */
    private String createWhat(AuditEntryBean auditEntryBean) {
        switch (auditEntryBean.getWhat()) {
        case AddPolicy:
            return i18n.format(AppMessages.ACTIVITY_ADD_POLICY);
        case BreakContract:
            if (auditEntryBean.getEntityType() == AuditEntityType.Service) {
                return i18n.format(AppMessages.ACTIVITY_BREAK_CONTRACT_WITH);
            } else {
                return i18n.format(AppMessages.ACTIVITY_BREAK_CONTRACT_FOR);
            }
        case Create:
            switch (auditEntryBean.getEntityType()) {
            case Application:
                return i18n.format(AppMessages.ACTIVITY_CREATE_APP);
            case Organization:
                return i18n.format(AppMessages.ACTIVITY_CREATE_ORG);
            case Plan:
                return i18n.format(AppMessages.ACTIVITY_CREATE_PLAN);
            case Service:
                return i18n.format(AppMessages.ACTIVITY_CREATE_SERVICE);
            default:
                return "??"; //$NON-NLS-1$
            }
        case CreateContract:
            break;
        case Delete:
            switch (auditEntryBean.getEntityType()) {
            case Application:
                return i18n.format(AppMessages.ACTIVITY_DELETE_APP);
            case Organization:
                return i18n.format(AppMessages.ACTIVITY_DELETE_ORG);
            case Plan:
                return i18n.format(AppMessages.ACTIVITY_DELETE_PLAN);
            case Service:
                return i18n.format(AppMessages.ACTIVITY_DELETE_SERVICE);
            default:
                return "??"; //$NON-NLS-1$
            }
        case Grant:
            return i18n.format(AppMessages.ACTIVITY_GRANT);
        case Publish:
            return i18n.format(AppMessages.ACTIVITY_PUBLISH);
        case Register:
            return i18n.format(AppMessages.ACTIVITY_REGISTER);
        case RemovePolicy:
            return i18n.format(AppMessages.ACTIVITY_REMOVE_POLICY);
        case Retire:
            return i18n.format(AppMessages.ACTIVITY_RETIRE);
        case Revoke:
            return i18n.format(AppMessages.ACTIVITY_REVOKE);
        case Unregister:
            return i18n.format(AppMessages.ACTIVITY_UNREGISTER);
        case Update:
            switch (auditEntryBean.getEntityType()) {
            case Application:
                return i18n.format(AppMessages.ACTIVITY_UPDATE_APP);
            case Organization:
                return i18n.format(AppMessages.ACTIVITY_UPDATE_ORG);
            case Plan:
                return i18n.format(AppMessages.ACTIVITY_UPDATE_PLAN);
            case Service:
                return i18n.format(AppMessages.ACTIVITY_UPDATE_SERVICE);
            default:
                return "??"; //$NON-NLS-1$
            }
        case UpdatePolicy:
            return i18n.format(AppMessages.ACTIVITY_UPDATE_POLICY);
        default:
            return "??"; //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Returns the fontawesome icon to use for the audit entry.
     * @param auditEntryBean
     */
    private String getIcon(AuditEntryBean auditEntryBean) {
        switch (auditEntryBean.getEntityType()) {
        case Application:
            return "gears"; //$NON-NLS-1$
        case Organization:
            return "shield"; //$NON-NLS-1$
        case Plan:
            return "bar-chart-o"; //$NON-NLS-1$
        case Service:
            return "puzzle-piece"; //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Creates a detail row for the audit entry bean.  Not all audit entries 
     * have additional details, so this may return null in that case.
     * @param auditEntryBean
     */
    private AbstractDetailPanel createDetailRow(AuditEntryBean auditEntryBean) {
        AbstractDetailPanel panel = null;
        if (auditEntryBean.getWhat() == AuditEntryType.Grant) {
            panel = grantPanelFactory.get();
        }
        if (auditEntryBean.getWhat() == AuditEntryType.Revoke) {
            panel = revokePanelFactory.get();
        }
        if (auditEntryBean.getWhat() == AuditEntryType.UpdatePolicy) {
            panel = updatePolicyPanelFactory.get();
        }
        if (auditEntryBean.getWhat() == AuditEntryType.AddPolicy) {
            panel = addPolicyPanelFactory.get();
        }
        if (auditEntryBean.getWhat() == AuditEntryType.RemovePolicy) {
            panel = removePolicyPanelFactory.get();
        }
        if (auditEntryBean.getWhat() == AuditEntryType.Update) {
            panel = updatePanelFactory.get();
        }
        
        if (panel != null) {
            panel.render(auditEntryBean);
        }
        return panel;
    }

    /**
     * Returns a span with some text indicating when the activity occurred.
     * 
     * @param auditEntryBean
     */
    private Label createWhen(AuditEntryBean auditEntryBean) {
        // TODO fix the when string - better formatting
        Label whenLabel = new Label(auditEntryBean.getWhen().toString());
        whenLabel.getElement().setClassName("apiman-timestamp"); //$NON-NLS-1$
        return whenLabel;
    }

    /**
     * @see com.google.gwt.user.client.TakesValue#getValue()
     */
    @Override
    public SearchResultsBean<AuditEntryBean> getValue() {
        throw new RuntimeException("Not implemented - should not be called."); //$NON-NLS-1$
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.events.MoreActivityItemsEvent.HasMoreActivityItemsHandlers#addMoreActivityItemsHandler(org.overlord.apiman.dt.ui.client.local.events.MoreActivityItemsEvent.Handler)
     */
    @Override
    public HandlerRegistration addMoreActivityItemsHandler(Handler handler) {
        return addHandler(handler, MoreActivityItemsEvent.getType());
    }
    
}