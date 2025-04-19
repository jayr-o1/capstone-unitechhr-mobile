const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Function to mark a user's email as verified
exports.verifyEmail = functions.https.onCall(async (data, context) => {
    try {
        // Security check - only allow authenticated users to verify their own email
        if (!context.auth) {
            throw new functions.https.HttpsError(
                "unauthenticated",
                "The function must be called while authenticated."
            );
        }

        const uid = data.uid;

        // Ensure the user is trying to verify their own email
        if (uid !== context.auth.uid) {
            throw new functions.https.HttpsError(
                "permission-denied",
                "Users can only verify their own email."
            );
        }

        // Verify the code from Firestore
        const verificationDoc = await admin
            .firestore()
            .collection("verification_codes")
            .doc(uid)
            .get();

        if (!verificationDoc.exists) {
            throw new functions.https.HttpsError(
                "not-found",
                "Verification record not found."
            );
        }

        const verificationData = verificationDoc.data();
        const storedCode = verificationData.code;
        const timestamp = verificationData.timestamp;
        const currentTime = Date.now();
        const providedCode = data.code;

        // Check if the code is valid and not expired (10 minutes validity)
        const TEN_MINUTES_IN_MS = 10 * 60 * 1000;
        if (storedCode !== providedCode) {
            throw new functions.https.HttpsError(
                "invalid-argument",
                "Invalid verification code."
            );
        }

        if (currentTime - timestamp > TEN_MINUTES_IN_MS) {
            throw new functions.https.HttpsError(
                "deadline-exceeded",
                "Verification code has expired. Please request a new one."
            );
        }

        // Mark the email as verified in Firebase Auth
        await admin.auth().updateUser(uid, {
            emailVerified: true,
        });

        // Update Firestore record to mark as verified
        await admin
            .firestore()
            .collection("verification_codes")
            .doc(uid)
            .update({
                verified: true,
            });

        return { success: true };
    } catch (error) {
        console.error("Error verifying email:", error);
        throw new functions.https.HttpsError(
            "internal",
            error.message || "An unknown error occurred while verifying email."
        );
    }
});

// Function to resend a verification code
exports.resendVerificationCode = functions.https.onCall(
    async (data, context) => {
        try {
            // Security check - only allow authenticated users to resend their own verification code
            if (!context.auth) {
                throw new functions.https.HttpsError(
                    "unauthenticated",
                    "The function must be called while authenticated."
                );
            }

            const uid = context.auth.uid;

            // Generate a new 6-digit verification code
            const verificationCode = Math.floor(
                100000 + Math.random() * 900000
            ).toString();

            // Update the verification code in Firestore
            await admin
                .firestore()
                .collection("verification_codes")
                .doc(uid)
                .update({
                    code: verificationCode,
                    timestamp: Date.now(),
                });

            return { code: verificationCode };
        } catch (error) {
            console.error("Error resending verification code:", error);
            throw new functions.https.HttpsError(
                "internal",
                error.message ||
                    "An unknown error occurred while resending verification code."
            );
        }
    }
);

// Function to send verification email
exports.sendVerificationEmail = functions.https.onCall(
    async (data, context) => {
        try {
            // Allow this function to be called without authentication
            // because it might be called during registration

            const { email, code, name } = data;

            if (!email || !code) {
                throw new functions.https.HttpsError(
                    "invalid-argument",
                    "Email and verification code are required."
                );
            }

            // Here you would normally integrate with an email service provider
            // like SendGrid, Mailgun, AWS SES, etc.
            // For this demo, we'll just log the attempt to send an email
            console.log(
                `Sending verification email to ${email} with code ${code}`
            );

            // For demo purposes, we're returning success without actually sending
            // an email since we're showing the code in the app UI

            /* Example of actual email sending code:
        const mailOptions = {
            from: '"UniTech HR" <noreply@unitechhr.com>',
            to: email,
            subject: 'Email Verification',
            html: `
                <h2>Welcome to UniTech HR, ${name}!</h2>
                <p>Your verification code is: <strong>${code}</strong></p>
                <p>This code will expire in 10 minutes.</p>
            `
        };
        
        await transporter.sendMail(mailOptions);
        */

            return { success: true };
        } catch (error) {
            console.error("Error sending verification email:", error);
            throw new functions.https.HttpsError(
                "internal",
                error.message ||
                    "An unknown error occurred while sending verification email."
            );
        }
    }
);
