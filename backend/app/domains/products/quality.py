import io


def assess_image_quality(content: bytes, mime_type: str) -> dict:
    if mime_type not in {"image/jpeg", "image/png"}:
        return {"status": "passed", "issues": [], "blur_score": None, "lighting_score": None,
                "crop_score": None, "glare_score": None, "readability_score": None}
    from PIL import Image, ImageFilter, ImageStat
    with Image.open(io.BytesIO(content)) as source:
        image = source.convert("L")
        width, height = image.size
        mean = ImageStat.Stat(image).mean[0]
        tonal_variance = ImageStat.Stat(image).var[0]
        edges = image.filter(ImageFilter.FIND_EDGES)
        edge_variance = ImageStat.Stat(edges).var[0]
        histogram = image.histogram()
        total = max(width * height, 1)
        glare = sum(histogram[245:]) / total
        blur_score = min(edge_variance / 600.0, tonal_variance / 500.0, 1.0)
        lighting_score = max(0.0, 1.0 - abs(mean - 135.0) / 135.0)
        crop_score = 1.0 if min(width, height) >= 600 else min(width, height) / 600.0
        glare_score = max(0.0, 1.0 - glare * 5.0)
        readability_score = min(blur_score, lighting_score, crop_score, glare_score)
        issues = []
        if blur_score < .18: issues.append("blur")
        if lighting_score < .35: issues.append("poor_lighting")
        if crop_score < .75: issues.append("cropping_or_low_resolution")
        if glare_score < .65: issues.append("glare")
        if readability_score < .18: issues.append("unreadable")
        status = "rejected" if "unreadable" in issues else ("needs_review" if issues else "passed")
        return {"status": status, "issues": issues, "blur_score": blur_score,
                "lighting_score": lighting_score, "crop_score": crop_score,
                "glare_score": glare_score, "readability_score": readability_score}
