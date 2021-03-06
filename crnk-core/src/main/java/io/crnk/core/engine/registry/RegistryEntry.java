package io.crnk.core.engine.registry;

import java.io.Serializable;

import io.crnk.core.engine.information.repository.ResourceRepositoryInformation;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.repository.RelationshipRepositoryAdapter;
import io.crnk.core.engine.internal.repository.ResourceRepositoryAdapter;
import io.crnk.core.queryspec.pagingspec.PagingBehavior;
import io.crnk.core.repository.ResourceRepositoryV2;
import io.crnk.legacy.internal.RepositoryMethodParameterProvider;

public interface RegistryEntry {


	@SuppressWarnings("unchecked")
	ResourceRepositoryAdapter getResourceRepository(RepositoryMethodParameterProvider parameterProvider);

	RelationshipRepositoryAdapter getRelationshipRepository(String fieldName,
			RepositoryMethodParameterProvider parameterProvider);

	@SuppressWarnings("unchecked")
	RelationshipRepositoryAdapter getRelationshipRepository(ResourceField field, RepositoryMethodParameterProvider
			parameterProvider);


	ResourceInformation getResourceInformation();

	ResourceRepositoryInformation getRepositoryInformation();

	RegistryEntry getParentRegistryEntry();


	/**
	 * @param parentRegistryEntry parent resource
	 */
	@Deprecated
	void setParentRegistryEntry(RegistryEntry parentRegistryEntry);


	/**
	 * Check the legacy is a parent of <b>this</b> {@link RegistryEntry}
	 * instance
	 *
	 * @param registryEntry parent to check
	 * @return true if the legacy is a parent
	 */
	boolean isParent(RegistryEntry registryEntry);


	/**
	 * @return we may or may should not have a public facing ResourceRepositoryAdapter
	 */
	@Deprecated
	ResourceRepositoryAdapter getResourceRepository();


	/**
	 * @return {@link ResourceRepositoryV2} facade to access the repository. Note that this is not the original
	 * {@link ResourceRepositoryV2}
	 * implementation backing the repository, but a facade that will also invoke all filters, decorators, etc. The actual
	 * repository may or may not be implemented with {@link ResourceRepositoryV2}.
	 * <p>
	 * Note that currently there is not (yet) any inclusion mechanism supported. This is currently done on a
	 * resource/document level only. But there might be some benefit to also be able to do it here on some occasions.
	 */
	<T, I extends Serializable> ResourceRepositoryV2<T, I> getResourceRepositoryFacade();


	PagingBehavior getPagingBehavior();
}
