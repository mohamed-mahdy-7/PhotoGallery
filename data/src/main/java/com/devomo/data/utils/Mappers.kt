package com.devomo.data.utils

import com.devomo.data.local.model.PhotoEntity
import com.devomo.data.remote.model.PhotoDto
import com.devomo.domain.models.Photo


fun PhotoDto.toDomain(): Photo {
    return Photo(
        id = id,
        imageUrl = src.large,
        photographer = photographer,
        photographerUrl = photographerUrl,
        avgColor = avgColor
    )
}

fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = id,
        imageUrl = imageUrl,
        photographer = photographer,
        photographerUrl = photographerUrl,
        avgColor = avgColor
    )
}

fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        imageUrl = imageUrl,
        photographer = photographer,
        photographerUrl = photographerUrl,
        avgColor = avgColor
    )
}