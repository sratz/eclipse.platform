package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * 
 */
public class ConfigurationPolicyModel extends ModelObject {

	
	

	private int policy;
	private List /* of FeatureReferenceModel */configuredFeatureReferences;
	private List /* of FeatureReferenceModel */unconfiguredFeatureReferences;

	/**
	 * Constructor for ConfigurationPolicyModel.
	 */
	public ConfigurationPolicyModel() {
		super();
	}

	/**
	 * Copy Constructor for ConfigurationPolicyModel.
	 */
	public ConfigurationPolicyModel(ConfigurationPolicyModel configPolicy) {
		super();
		this.policy = configPolicy.getPolicy();
		configuredFeatureReferences = new ArrayList(0);
		configuredFeatureReferences.addAll(Arrays.asList(configPolicy.getConfiguredFeaturesModel()));
		unconfiguredFeatureReferences = new ArrayList(0);
		unconfiguredFeatureReferences.addAll(Arrays.asList(configPolicy.getUnconfiguredFeaturesModel()));
	}


	
	/**
	 * @since 2.0
	 */
	public int getPolicy() {
		return policy;
	}

	/**
	 * Sets the policy.
	 * @param policy The policy to set
	 */
	public void setPolicy(int policy) {
		assertIsWriteable();
		this.policy = policy;
	}

	/**
	 * @since 2.0
	 */
	public FeatureReferenceModel[] getConfiguredFeaturesModel() {
		if (configuredFeatureReferences==null)
			return new FeatureReferenceModel[0];
		return (FeatureReferenceModel[]) configuredFeatureReferences.toArray(arrayTypeFor(configuredFeatureReferences));
	}

	/**
	 * @since 2.0
	 */
	public FeatureReferenceModel[] getUnconfiguredFeaturesModel() {
	if (configuredFeatureReferences==null)
			return new FeatureReferenceModel[0];			
		return (FeatureReferenceModel[]) unconfiguredFeatureReferences.toArray(arrayTypeFor(unconfiguredFeatureReferences));		
	}

	/**
	 * @since 2.0
	 */
	public boolean isConfigured(FeatureReferenceModel feature) {
		boolean result = false;
		// return true if the feature is part of the configured list
		Iterator iter = configuredFeatureReferences.iterator();
		String featureURLString = feature.getURL().toExternalForm();
		while (iter.hasNext() && !result) {
			FeatureReferenceModel element = (FeatureReferenceModel) iter.next();
			if (element.getURL().toExternalForm().trim().equalsIgnoreCase(featureURLString)) {
				result = true;
			}
		}
		return result;
	}


	/**
	 * 
	 */
	private void remove(FeatureReferenceModel feature, List list) {
		String featureURLString = feature.getURL().toExternalForm();
		boolean found = false;
		Iterator iter = list.iterator();
		while (iter.hasNext() && !found) {
			FeatureReferenceModel element = (FeatureReferenceModel) iter.next();
			if (element.getURL().toExternalForm().trim().equalsIgnoreCase(featureURLString)) {
				list.remove(element);
				found = true;
			}
		}
	}

	/**
	 * returns an array of string corresponding to plugins file
	 */
	/*package*/

	
	/**
	 * 
	 */
	private void add(FeatureReferenceModel feature, List list) {
		String featureURLString = feature.getURL().toExternalForm();
		boolean found = false;
		Iterator iter = list.iterator();
		while (iter.hasNext() && !found) {
			FeatureReferenceModel element = (FeatureReferenceModel) iter.next();
			if (element.getURL().toExternalForm().trim().equalsIgnoreCase(featureURLString)) {
				found = true;
			}
		}

		if (!found) {
			list.add(feature);
		}
	}

	/**
	 * adds a feature in the configuredReference list
	 * also used by the parser to avoid creating another activity
	 */
	public void addConfiguredFeatureReference(FeatureReferenceModel feature) {
		assertIsWriteable();
		
		if (configuredFeatureReferences == null)
			this.configuredFeatureReferences = new ArrayList();
		if (!configuredFeatureReferences.contains(feature))
			this.add(feature, configuredFeatureReferences);	

		// when user configure a feature,
		// we have to remove it from unconfigured feature if it exists
		// because the user doesn't know...
		if (unconfiguredFeatureReferences != null) {
			remove(feature, unconfiguredFeatureReferences);
		}

	}

	/**
	 * adds a feature in teh list
	 * also used by the parser to avoid creating another activity
	 */
	public void addUnconfiguredFeatureReference(FeatureReferenceModel feature) {
		assertIsWriteable();
		if (unconfiguredFeatureReferences == null)
			this.unconfiguredFeatureReferences = new ArrayList();
		if (unconfiguredFeatureReferences.contains(feature))
			this.add(feature, unconfiguredFeatureReferences);	

		// an unconfigured feature is always from a configured one no ?
		// unless it was parsed right ?
		if (configuredFeatureReferences != null) {
			remove(feature, configuredFeatureReferences);
		}
	}

	
}