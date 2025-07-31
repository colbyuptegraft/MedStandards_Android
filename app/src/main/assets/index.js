var pdfDoc = null;
var renderingInProgress = false;

function createPage(pageNumber) {
    var canvas = document.createElement("canvas");
    canvas.style.display = "block";
    canvas.style.margin = "auto";
    canvas.setAttribute("data-page-number", pageNumber); // Add attribute for page identification
    
    // Don't add canvas to DOM immediately, return it instead
    return canvas;
}

function insertPageInOrder(canvas, pageNumber) {
    // Find the correct position to insert the page
    var existingCanvases = document.querySelectorAll('canvas[data-page-number]');
    var inserted = false;
    
    for (var i = 0; i < existingCanvases.length; i++) {
        var existingPageNumber = parseInt(existingCanvases[i].getAttribute('data-page-number'));
        if (pageNumber < existingPageNumber) {
            document.body.insertBefore(canvas, existingCanvases[i]);
            inserted = true;
            break;
        }
    }
    
    // If no insertion position found, append to the end
    if (!inserted) {
        document.body.appendChild(canvas);
    }
}

// Improved single page rendering function
function renderPage(num) {
    return new Promise((resolve, reject) => {
        if (!pdfDoc) {
            reject(new Error("PDF document not loaded"));
            return;
        }

        pdfDoc.getPage(num).then(function (page) {
            var screenWidth = window.innerWidth;
            var viewport = page.getViewport({ scale: 1 });

            var scale = screenWidth / viewport.width;
            viewport = page.getViewport({ scale: scale });

            var canvas = createPage(num);
            var ctx = canvas.getContext('2d');

            var outputScale = Math.min(window.devicePixelRatio || 1, 2);
            canvas.width = Math.floor(viewport.width * outputScale);
            canvas.height = Math.floor(viewport.height * outputScale);
            canvas.style.width = Math.floor(viewport.width) + "px";
            canvas.style.height = Math.floor(viewport.height) + "px";

            var renderContext = {
                canvasContext: ctx,
                viewport: viewport,
                transform: [outputScale, 0, 0, outputScale, 0, 0]
            };

            page.render(renderContext).promise.then(() => {
                // Insert page in correct order
                insertPageInOrder(canvas, num);
                console.log(`Page ${num} rendered and inserted in correct order`);
                resolve();
            }).catch((error) => {
                console.error(`Error rendering page ${num}:`, error);
                reject(error);
            });
        }).catch((error) => {
            console.error(`Error loading page ${num}:`, error);
            reject(error);
        });
    });
}

// Simplified function for rendering all pages
async function renderAllPagesSequentially() {
    if (renderingInProgress) {
        console.log("Rendering already in progress, skipping...");
        return;
    }
    
    renderingInProgress = true;
    console.log(`Starting rendering of ${pdfDoc.numPages} pages`);
    
    try {
        // Render all pages sequentially (but faster than before)
        for (var i = 1; i <= pdfDoc.numPages; i++) {
            console.log(`Rendering page ${i} of ${pdfDoc.numPages}...`);
            await renderPage(i);
            
            // Minimal pause for UI responsiveness
            await new Promise(resolve => setTimeout(resolve, 10));
        }
        
        console.log("All pages rendered successfully");
        
    } catch (error) {
        console.error("Error during rendering:", error);
    } finally {
        renderingInProgress = false;
    }
}



function scrollToPage(pageIndex) {
    const canvasList = document.querySelectorAll('canvas[data-page-number]');
    
    // Find canvas with the required page number
    let targetCanvas = null;
    for (let i = 0; i < canvasList.length; i++) {
        if (parseInt(canvasList[i].getAttribute('data-page-number')) === pageIndex + 1) {
            targetCanvas = canvasList[i];
            break;
        }
    }

    if (!targetCanvas) {
        console.error(`Canvas for page ${pageIndex + 1} not found`);
        return;
    }

    const topOffset = targetCanvas.getBoundingClientRect().top + window.scrollY;

    window.scrollTo({
        top: topOffset,
        behavior: "smooth"
    });
}

function scrollToWord(x, y) {
    window.scrollTo({
        left: x,
        top: y,
        behavior: "smooth"
    });
}

function receivePDF(base64String) {
    console.log("Received Base64 PDF, length:", base64String.length);

    // Reset state before loading new PDF
    renderingInProgress = false;

    try {
        var binaryString = atob(base64String);
        var bytes = new Uint8Array(binaryString.length);
        for (var i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        pdfjsLib.getDocument({ data: bytes.buffer }).promise.then(function (pdf) {
            pdfDoc = pdf;
            console.log(`PDF loaded successfully, total pages: ${pdf.numPages}`);
            
            // Clear container before rendering
            document.body.innerHTML = "";
            
            // Start sequential rendering
            renderAllPagesSequentially().then(() => {
                console.log("PDF rendering completed");
            }).catch((error) => {
                console.error("Failed to render PDF:", error);
            });
            
        }).catch(function (error) {
            console.error("Error loading PDF:", error);
        });

    } catch (e) {
        console.error("Error processing Base64:", e);
    }
}
