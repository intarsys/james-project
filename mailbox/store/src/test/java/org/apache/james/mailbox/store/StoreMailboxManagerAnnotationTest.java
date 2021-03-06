/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class StoreMailboxManagerAnnotationTest {
    private static final MailboxAnnotation PRIVATE_ANNOTATION = MailboxAnnotation.newInstance("/private/comment", "My private comment");
    private static final MailboxAnnotation SHARED_ANNOTATION =  MailboxAnnotation.newInstance("/shared/comment", "My shared comment");
    private static final Set<String> KEYS = ImmutableSet.of("/private/comment");

    private static final List<MailboxAnnotation> ANNOTATIONS = ImmutableList.of(PRIVATE_ANNOTATION, SHARED_ANNOTATION);
    private static final List<MailboxAnnotation> ANNOTATIONS_WITH_NIL_ENTRY = ImmutableList.of(PRIVATE_ANNOTATION, MailboxAnnotation.nil("/shared/comment"));

    @Mock private MailboxSessionMapperFactory mailboxSessionMapperFactory;
    @Mock private Authenticator authenticator;
    @Mock private MailboxACLResolver aclResolver;
    @Mock private GroupMembershipResolver groupMembershipResolver;
    
    @Mock private MailboxSession session;
    @Mock private MailboxMapper mailboxMapper;
    @Mock private AnnotationMapper annotationMapper;
    @Mock private MailboxPath mailboxPath;
    @Mock private Mailbox mailbox;
    @Mock private MessageParser messageParser;
    @Mock private MailboxId mailboxId;

    @InjectMocks
    private StoreMailboxManager storeMailboxManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mailboxSessionMapperFactory.getMailboxMapper(eq(session))).thenReturn(mailboxMapper);
        when(mailbox.getMailboxId()).thenReturn(mailboxId);
        when(mailboxSessionMapperFactory.getAnnotationMapper(eq(mailboxId), eq(session))).thenReturn(annotationMapper);

        storeMailboxManager = new StoreMailboxManager(mailboxSessionMapperFactory, authenticator, aclResolver, groupMembershipResolver, messageParser);
    }

    @Test(expected = MailboxException.class)
    public void updateAnnotationsShouldThrowExceptionWhenDoesNotLookupMailbox() throws Exception {
        doThrow(MailboxException.class).when(mailboxMapper).findMailboxByPath(eq(mailboxPath));
        storeMailboxManager.updateAnnotations(mailboxPath, session, ImmutableList.of(PRIVATE_ANNOTATION));
    }

    @Test
    public void updateAnnotationsShouldCallAnnotationMapperToInsertAnnotation() throws Exception {
        when(mailboxMapper.findMailboxByPath(eq(mailboxPath))).thenReturn(mailbox);
        storeMailboxManager.updateAnnotations(mailboxPath, session, ANNOTATIONS);

        verify(annotationMapper, times(2)).insertAnnotation(any(MailboxAnnotation.class));
    }

    @Test
    public void updateAnnotationsShouldCallAnnotationMapperToDeleteAnnotation() throws Exception {
        when(mailboxMapper.findMailboxByPath(eq(mailboxPath))).thenReturn(mailbox);
        storeMailboxManager.updateAnnotations(mailboxPath, session, ANNOTATIONS_WITH_NIL_ENTRY);

        verify(annotationMapper, times(1)).insertAnnotation(eq(PRIVATE_ANNOTATION));
        verify(annotationMapper, times(1)).deleteAnnotation(eq("/shared/comment"));
    }

    @Test(expected = MailboxException.class)
    public void getAllAnnotationsShouldThrowExceptionWhenDoesNotLookupMailbox() throws Exception {
        doThrow(MailboxException.class).when(mailboxMapper).findMailboxByPath(eq(mailboxPath));
        storeMailboxManager.getAllAnnotations(mailboxPath, session);
    }

    @Test
    public void getAllAnnotationsShouldReturnEmptyForNonStoredAnnotation() throws Exception {
        when(mailboxMapper.findMailboxByPath(eq(mailboxPath))).thenReturn(mailbox);
        when(annotationMapper.getAllAnnotations()).thenReturn(Collections.<MailboxAnnotation> emptyList());

        assertThat(storeMailboxManager.getAllAnnotations(mailboxPath, session)).isEmpty();
    }

    @Test
    public void getAllAnnotationsShouldReturnStoredAnnotation() throws Exception {
        when(mailboxMapper.findMailboxByPath(eq(mailboxPath))).thenReturn(mailbox);
        when(annotationMapper.getAllAnnotations()).thenReturn(ANNOTATIONS);

        assertThat(storeMailboxManager.getAllAnnotations(mailboxPath, session)).isEqualTo(ANNOTATIONS);
    }

    @Test(expected = MailboxException.class)
    public void getAnnotationsByKeysShouldThrowExceptionWhenDoesNotLookupMailbox() throws Exception {
        doThrow(MailboxException.class).when(mailboxMapper).findMailboxByPath(eq(mailboxPath));
        storeMailboxManager.getAnnotationsByKeys(mailboxPath, session, KEYS);
    }

    @Test
    public void getAnnotationsByKeysShouldRetrieveStoreAnnotationsByKey() throws Exception {
        when(mailboxMapper.findMailboxByPath(eq(mailboxPath))).thenReturn(mailbox);
        when(annotationMapper.getAnnotationsByKeys(KEYS)).thenReturn(ANNOTATIONS);

        assertThat(storeMailboxManager.getAnnotationsByKeys(mailboxPath, session, KEYS)).isEqualTo(ANNOTATIONS);
    }

}
