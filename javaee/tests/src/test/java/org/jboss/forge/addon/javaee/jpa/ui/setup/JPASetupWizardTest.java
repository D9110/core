/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.javaee.jpa.ui.setup;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.PersistenceMetaModelFacet;
import org.jboss.forge.addon.javaee.jpa.PersistenceOperations;
import org.jboss.forge.addon.javaee.jpa.containers.CustomJTAContainer;
import org.jboss.forge.addon.javaee.jpa.containers.JBossEAP6Container;
import org.jboss.forge.addon.javaee.jpa.providers.HibernateMetaModelProvider;
import org.jboss.forge.addon.javaee.jpa.providers.JavaEEDefaultProvider;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.AbstractCommandExecutionListener;
import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.ui.test.UITestHarness;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.descriptor.api.common.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.common.PersistenceUnitCommonType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JPASetupWizardTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.addon:ui"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:javaee"),
            @AddonDependency(name = "org.jboss.forge.addon:maven")
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness")
               );
   }

   @Inject
   private JavaEEDefaultProvider defaultProvider;

   @Inject
   private CustomJTAContainer customJTAProvider;

   @Inject
   private JBossEAP6Container eap6Container;

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private UITestHarness testHarness;

   @Test
   public void testSetup() throws Exception
   {
      final Project project = projectFactory.createTempProject();
      WizardCommandController tester = testHarness.createWizardController(JPASetupWizard.class,
               project.getProjectRoot());

      tester.initialize();

      Assert.assertFalse(tester.canMoveToPreviousStep());
      // Setting UI values
      tester.setValueFor("provider", defaultProvider);
      tester.setValueFor("container", customJTAProvider);
      Assert.assertTrue(tester.canMoveToNextStep());

      tester.next().initialize();

      Assert.assertFalse(tester.isValid());
      tester.setValueFor("dataSourceName", "java:jboss:jta-ds");
      Assert.assertTrue(tester.isValid());
      final AtomicInteger counter = new AtomicInteger();
      tester.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener()
      {
         @Override
         public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result)
         {
            counter.incrementAndGet();
         }
      });
      tester.execute();
      // Ensure that the two pages were invoked
      Assert.assertEquals(2, counter.get());

      // Check SUT values
      PersistenceCommonDescriptor config = (PersistenceCommonDescriptor) project.getFacet(JPAFacet.class).getConfig();
      List<PersistenceUnitCommonType> allUnits = config.getAllPersistenceUnit();
      Assert.assertEquals("java:jboss:jta-ds", allUnits.get(0).getJtaDataSource());
   }

   @Test
   public void testSetupDuplicateUnitName() throws Exception
   {
      // Execute SUT
      final Project project = projectFactory.createTempProject();
      WizardCommandController tester = testHarness.createWizardController(JPASetupWizard.class,
               project.getProjectRoot());

      tester.initialize();

      Assert.assertFalse(tester.canMoveToPreviousStep());
      // Setting UI values
      tester.setValueFor("provider", defaultProvider);
      tester.setValueFor("container", eap6Container);
      Assert.assertTrue(tester.canMoveToNextStep());

      tester.next().initialize();

      Result result = tester.execute();
      Assert.assertFalse(result instanceof Failed);

      // Check SUT values
      PersistenceCommonDescriptor config = (PersistenceCommonDescriptor) project.getFacet(JPAFacet.class).getConfig();
      List<PersistenceUnitCommonType> allUnits = config.getAllPersistenceUnit();
      Assert.assertEquals(PersistenceOperations.DEFAULT_UNIT_NAME, allUnits.get(0).getName());

      WizardCommandController tester2 = testHarness.createWizardController(JPASetupWizard.class,
               project.getProjectRoot());

      // Launch
      tester2.initialize();

      Assert.assertFalse(tester2.canMoveToPreviousStep());
      // Setting UI values
      tester2.setValueFor("provider", defaultProvider);
      tester2.setValueFor("container", eap6Container);
      Assert.assertTrue(tester2.canMoveToNextStep());

      tester2.next().initialize();

      result = tester2.execute();
      Assert.assertFalse(result instanceof Failed);

      // Check SUT values
      config = (PersistenceCommonDescriptor) project.getFacet(JPAFacet.class).getConfig();
      allUnits = config.getAllPersistenceUnit();
      Assert.assertEquals(PersistenceOperations.DEFAULT_UNIT_NAME, allUnits.get(0).getName());
      Assert.assertEquals(PersistenceOperations.DEFAULT_UNIT_NAME + "-1", allUnits.get(1).getName());
   }

   @Test
   public void testSetupMetadata() throws Exception
   {
      // Execute SUT
      final Project project = projectFactory.createTempProject();
      WizardCommandController tester = testHarness.createWizardController(JPASetupWizard.class,
               project.getProjectRoot());

      tester.initialize();

      Assert.assertFalse(tester.canMoveToPreviousStep());
      // Setting UI values
      tester.setValueFor("provider", defaultProvider);
      tester.setValueFor("container", customJTAProvider);
      tester.setValueFor("configureMetadata", Boolean.TRUE);
      Assert.assertTrue(tester.canMoveToNextStep());

      tester.next().initialize();

      tester.setValueFor("dataSourceName", "java:jboss:jta-ds");
      final AtomicInteger counter = new AtomicInteger();
      tester.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener()
      {
         @Override
         public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result)
         {
            counter.incrementAndGet();
         }
      });
      tester.execute();
      // Ensure that the two pages were invoked
      Assert.assertEquals(2, counter.get());

      // Check SUT values
      PersistenceCommonDescriptor config = (PersistenceCommonDescriptor) project.getFacet(JPAFacet.class).getConfig();
      List<PersistenceUnitCommonType> allUnits = config.getAllPersistenceUnit();

      Assert.assertEquals("java:jboss:jta-ds", allUnits.get(0).getJtaDataSource());

      Assert.assertTrue(project.hasFacet(PersistenceMetaModelFacet.class));
      PersistenceMetaModelFacet facet = project.getFacet(PersistenceMetaModelFacet.class);
      Assert.assertEquals(new HibernateMetaModelProvider().getProcessor(), facet.getProcessor());
   }
}
