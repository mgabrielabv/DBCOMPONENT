package com.MariaBermudez.db;
public record DBQueryResult<T>(T result, int affectedRows) {}