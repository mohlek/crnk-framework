package io.crnk.client.internal;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.client.ClientException;
import io.crnk.client.CrnkClient;
import io.crnk.client.TransportException;
import io.crnk.client.http.HttpAdapter;
import io.crnk.client.http.HttpAdapterRequest;
import io.crnk.client.http.HttpAdapterResponse;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpHeaders;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.internal.exception.ExceptionMapperRegistry;
import io.crnk.core.engine.internal.utils.JsonApiUrlBuilder;
import io.crnk.core.utils.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStubBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientStubBase.class);

	protected CrnkClient client;

	protected JsonApiUrlBuilder urlBuilder;

	public ClientStubBase(CrnkClient client, JsonApiUrlBuilder urlBuilder) {
		this.client = client;
		this.urlBuilder = urlBuilder;
	}

	protected Object executeGet(String requestUrl, ResponseType responseType) {
		return execute(requestUrl, responseType, HttpMethod.GET, null);
	}

	protected Object executeDelete(String requestUrl) {
		return execute(requestUrl, ResponseType.NONE, HttpMethod.DELETE, null);
	}

	protected Object execute(String url, ResponseType responseType, HttpMethod method, String requestBody) {
		try {

			HttpAdapter httpAdapter = client.getHttpAdapter();
			HttpAdapterRequest request = httpAdapter.newRequest(url, method, requestBody);

			LOGGER.debug("requesting {} {}", method, url);
			if (requestBody != null) {
				LOGGER.debug("request body: {}", requestBody);
			}

			if (method == HttpMethod.POST || method == HttpMethod.PATCH) {
				request.header("Content-Type", HttpHeaders.JSONAPI_CONTENT_TYPE + "; charset=" +
						HttpHeaders.DEFAULT_CHARSET);
			}
			request.header("Accept", HttpHeaders.JSONAPI_CONTENT_TYPE);

			HttpAdapterResponse response = request.execute();

			if (!response.isSuccessful()) {
				throw handleError(response);
			}

			String body = response.body();
			LOGGER.debug("response body: {}", body);
			ObjectMapper objectMapper = client.getObjectMapper();

			if (responseType != ResponseType.NONE) {
				Document document = objectMapper.readValue(body, Document.class);

				ClientDocumentMapper documentMapper = client.getDocumentMapper();
				return documentMapper.fromDocument(document, responseType == ResponseType.RESOURCES);
			}
			return null;
		}
		catch (IOException e) {
			throw new TransportException(e);
		}
	}

	protected RuntimeException handleError(HttpAdapterResponse response) throws IOException {
		ErrorResponse errorResponse = null;
		String body = response.body();
		String contentType = response.getResponseHeader(HttpHeaders.HTTP_CONTENT_TYPE);
		if (body.length() > 0 && contentType != null && contentType.toLowerCase().contains(HttpHeaders.JSONAPI_CONTENT_TYPE)) {

			ObjectMapper objectMapper = client.getObjectMapper();
			Document document = objectMapper.readValue(body, Document.class);
			if (document.getErrors() != null && !document.getErrors().isEmpty()) {
				errorResponse = new ErrorResponse(document.getErrors(), response.code());
			}
		}
		if (errorResponse == null) {
			errorResponse = new ErrorResponse(null, response.code());
		}

		ExceptionMapperRegistry exceptionMapperRegistry = client.getExceptionMapperRegistry();
		Optional<ExceptionMapper<?>> mapper = (Optional) exceptionMapperRegistry.findMapperFor(errorResponse);
		if (mapper.isPresent()) {
			Throwable throwable = mapper.get().fromErrorResponse(errorResponse);
			if (throwable instanceof RuntimeException) {
				return (RuntimeException) throwable;
			}
			else {
				return new ClientException(response.code(), response.message(), throwable);
			}
		}
		else {
			return new ClientException(response.code(), response.message());
		}
	}

	public enum ResponseType {
		NONE, RESOURCE, RESOURCES
	}
}