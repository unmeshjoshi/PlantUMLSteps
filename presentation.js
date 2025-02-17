document.addEventListener("DOMContentLoaded", function () {
    const prevButton = document.getElementById("prev-button");
    const nextButton = document.getElementById("next-button");
    let currentSlideIndex = 0; // Tracks the current slide
    let currentStepIndex = 0; // Tracks the current step within a slide

    const slides = document.querySelectorAll(".slide");

    function updateButtonStates() {
        // Enable/disable previous button based on current position
        prevButton.disabled = currentSlideIndex === 0 && currentStepIndex === 0;

        // Get current slide and its steps
        const currentSlide = slides[currentSlideIndex];
        const steps = currentSlide.querySelectorAll(".step");

        // Enable/disable next button based on current position
        const isLastSlide = currentSlideIndex === slides.length - 1;
        const isLastStep = steps.length > 0 ? currentStepIndex === steps.length - 1 : true;
        nextButton.disabled = isLastSlide && isLastStep;
    }

    function showSlide(index) {
        slides.forEach((slide, i) => {
            slide.style.display = i === index ? "block" : "none";
        });
        currentStepIndex = 0; // Reset step index when changing slides
        showStep(slides[index]);
        updateButtonStates();
    }

    function showStep(slide) {
        const steps = Array.from(slide.querySelectorAll(".step"))
            .sort((a, b) => parseInt(a.getAttribute("step_index")) - parseInt(b.getAttribute("step_index"))); // Sort steps by step_index

        steps.forEach((step, i) => {
            step.style.display = i === currentStepIndex ? "block" : "none";
        });
        updateButtonStates();
    }

    function nextStep() {
        const slide = slides[currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (currentStepIndex < steps.length - 1) {
            currentStepIndex++;
            showStep(slide);
        } else {
            // Move to the next slide if no more steps
            if (currentSlideIndex < slides.length - 1) {
                currentSlideIndex++;
                showSlide(currentSlideIndex);
            }
        }
    }

    function prevStep() {
        const slide = slides[currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (currentStepIndex > 0) {
            currentStepIndex--;
            showStep(slide);
        } else {
            // Move to the previous slide if on the first step
            if (currentSlideIndex > 0) {
                currentSlideIndex--;
                showSlide(currentSlideIndex);

                // Show the last step of the new current slide if it has steps
                const newSlide = slides[currentSlideIndex];
                const newSteps = newSlide.querySelectorAll(".step");
                if (newSteps.length > 0) {
                    currentStepIndex = newSteps.length - 1;
                    showStep(newSlide);
                }
            }
        }
    }

    // Initialize by showing the first slide
    showSlide(currentSlideIndex);

    // Attach the functions to the navigation buttons
    document.getElementById("next-button").addEventListener("click", nextStep);
    document.getElementById("prev-button").addEventListener("click", prevStep);
});